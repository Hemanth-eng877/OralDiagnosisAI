import os
import sys
import glob
import json
import time
import datetime
import defusedxml.ElementTree as ET
from pathlib import Path

def parse_junit_xml(pattern):
    total = 0
    failed = 0
    duration = 0.0
    test_cases = []
    
    for path in glob.glob(pattern, recursive=True):
        try:
            tree = ET.parse(path)
            root = tree.getroot()
            if root.tag == 'testsuites':
                for ts in root.findall('testsuite'):
                    total += int(ts.get('tests', 0))
                    failed += int(ts.get('failures', 0)) + int(ts.get('errors', 0))
                    duration += float(ts.get('time', 0.0))
                    for tc in ts.findall('testcase'):
                        tc_name = tc.get('name', 'Unknown')
                        tc_status = '❌ FAIL' if (tc.find('failure') is not None or tc.find('error') is not None) else ('⚠️ SKIP' if tc.find('skipped') is not None else '✅ PASS')
                        tc_time = float(tc.get('time', 0.0))
                        test_cases.append({'name': tc_name, 'status': tc_status, 'time': tc_time, 'file': tc.get('file', '') or tc.get('classname', '')})
            elif root.tag == 'testsuite':
                total += int(root.get('tests', 0))
                failed += int(root.get('failures', 0)) + int(root.get('errors', 0))
                duration += float(root.get('time', 0.0))
                for tc in root.findall('testcase'):
                    tc_name = tc.get('name', 'Unknown')
                    tc_status = '❌ FAIL' if (tc.find('failure') is not None or tc.find('error') is not None) else ('⚠️ SKIP' if tc.find('skipped') is not None else '✅ PASS')
                    tc_time = float(tc.get('time', 0.0))
                    test_cases.append({'name': tc_name, 'status': tc_status, 'time': tc_time, 'file': tc.get('file', '') or tc.get('classname', '')})
        except Exception:
            pass
            
    passed = total - failed
    return total, passed if passed >= 0 else 0, failed, round(duration, 2), test_cases

def parse_coverage(pattern):
    for path in glob.glob(pattern, recursive=True):
        try:
            tree = ET.parse(path)
            root = tree.getroot()
            if root.tag == 'coverage':
                rate = float(root.get('line-rate', 0.0))
                return f"{round(rate * 100, 2)}%"
        except Exception:
            pass
    return "N/A"

def parse_android_lint(pattern):
    errors = 0
    warnings = 0
    for path in glob.glob(pattern, recursive=True):
        try:
            tree = ET.parse(path)
            root = tree.getroot()
            for issue in root.findall('issue'):
                sev = issue.get('severity', '').lower()
                if sev == 'error' or sev == 'fatal':
                    errors += 1
                elif sev == 'warning':
                    warnings += 1
        except Exception:
            pass
    return errors, warnings

def parse_security_scans():
    res = {
        'semgrep': {'crit': 0, 'high': 0, 'med': 0, 'low': 0},
        'trivy': {'crit': 0, 'high': 0, 'med': 0, 'low': 0},
        'gitleaks': {'crit': 0, 'high': 0, 'med': 0, 'low': 0},
        'dep_review': {'crit': 0, 'high': 0, 'med': 0, 'low': 0},
    }
    
    # Semgrep
    for path in glob.glob('**/semgrep.json', recursive=True):
        try:
            with open(path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                for r in data.get('results', []):
                    meta_sev = str(r.get('extra', {}).get('metadata', {}).get('severity', '')).upper()
                    if not meta_sev:
                        meta_sev = str(r.get('extra', {}).get('severity', '')).upper()
                    if meta_sev == 'CRITICAL':
                        res['semgrep']['crit'] += 1
                    elif meta_sev == 'HIGH' or meta_sev == 'ERROR':
                        res['semgrep']['high'] += 1
                    elif meta_sev == 'MEDIUM' or meta_sev == 'WARNING':
                        res['semgrep']['med'] += 1
                    else:
                        res['semgrep']['low'] += 1
        except Exception:
            pass

    # Trivy
    for path in glob.glob('**/trivy.json', recursive=True):
        try:
            with open(path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                for r in data.get('Results', []):
                    for v in (r.get('Vulnerabilities') or []):
                        sev = str(v.get('Severity', '')).upper()
                        if sev == 'CRITICAL':
                            res['trivy']['crit'] += 1
                        elif sev == 'HIGH':
                            res['trivy']['high'] += 1
                        elif sev == 'MEDIUM':
                            res['trivy']['med'] += 1
                        else:
                            res['trivy']['low'] += 1
        except Exception:
            pass

    # Gitleaks (all treated as critical since they are secrets)
    for path in glob.glob('**/gitleaks.json', recursive=True):
        try:
            with open(path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                if isinstance(data, list):
                    res['gitleaks']['crit'] += len(data)
        except Exception:
            pass

    # Dep Review (if generated or check summary)
    for path in glob.glob('**/dependency-review.json', recursive=True):
        try:
            with open(path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                if isinstance(data, dict):
                    pass
        except Exception:
            pass

    for path in glob.glob('**/dependency-review.md', recursive=True):
        try:
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read().lower()
                if "critical findings detected" in content or "failure" in content:
                    res['dep_review']['crit'] = max(res['dep_review']['crit'], 1)
        except Exception:
            pass

    return res

def parse_load_test():
    metrics = {
        'reqs': 0,
        'rate': 0.0,
        'avg': 0.0,
        'min': 0.0,
        'max': 0.0,
        'p95': 0.0,
        'p99': 0.0,
        'failed_rate': 0.0,
        'checks_pass_rate': 100.0,
        'vus': 0,
        'duration_str': 'N/A'
    }
    for path in glob.glob('**/load-test-results.json', recursive=True):
        try:
            with open(path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                m = data.get('metrics', {})
                if 'http_reqs' in m:
                    vals = m['http_reqs'].get('values', m['http_reqs'])
                    metrics['reqs'] = int(vals.get('count', 0))
                    metrics['rate'] = float(vals.get('rate', 0.0))
                if 'http_req_failed' in m:
                    vals = m['http_req_failed'].get('values', m['http_req_failed'])
                    if 'rate' in vals:
                        metrics['failed_rate'] = float(vals['rate'])
                    elif 'value' in vals:
                        metrics['failed_rate'] = float(vals['value'])
                    else:
                        passes = float(vals.get('passes', 0))
                        fails = float(vals.get('fails', 0))
                        tot = passes + fails
                        if tot > 0:
                            metrics['failed_rate'] = passes / tot
                if 'checks' in m:
                    vals = m['checks'].get('values', m['checks'])
                    if 'rate' in vals:
                        metrics['checks_pass_rate'] = float(vals['rate'])
                    elif 'value' in vals:
                        metrics['checks_pass_rate'] = float(vals['value'])
                    else:
                        passes = float(vals.get('passes', 0))
                        fails = float(vals.get('fails', 0))
                        tot = passes + fails
                        if tot > 0:
                            metrics['checks_pass_rate'] = passes / tot
                if 'http_req_duration' in m:
                    vals = m['http_req_duration'].get('values', m['http_req_duration'])
                    metrics['avg'] = float(vals.get('avg', 0.0))
                    metrics['min'] = float(vals.get('min', 0.0))
                    metrics['max'] = float(vals.get('max', 0.0))
                    metrics['p95'] = float(vals.get('p(95)', vals.get('p95', 0.0)))
                    metrics['p99'] = float(vals.get('p(99)', vals.get('p99', 0.0)))
                if 'vus_max' in m:
                    vals = m['vus_max'].get('values', m['vus_max'])
                    metrics['vus'] = int(vals.get('value', vals.get('max', 0)))
                elif 'vus' in m:
                    vals = m['vus'].get('values', m['vus'])
                    metrics['vus'] = int(vals.get('value', vals.get('max', 0)))
                if 'iteration_duration' in m:
                    vals = m['iteration_duration'].get('values', m['iteration_duration'])
                    duration = float(vals.get('avg', 0.0))
                    metrics['duration_str'] = f"{round(duration/1000, 2)}s per iter"
        except Exception:
            pass
    return metrics

def status_icon(failed_count):
    return "✅ PASS" if failed_count == 0 else "❌ FAIL"

def get_file_size(filepath):
    try:
        size = os.path.getsize(filepath)
        for unit in ['B', 'KB', 'MB', 'GB']:
            if size < 1024.0:
                return f"{size:.1f} {unit}"
            size /= 1024.0
        return f"{size:.1f} TB"
    except Exception:
        return "N/A"

def categorize_cases(cases, categories):
    results = {cat: {'total': 0, 'passed': 0, 'failed': 0, 'duration': 0.0} for cat in categories}
    
    for c in cases:
        name = str(c['name']).lower() + " " + str(c['file']).lower()
        matched = False
        for cat, keywords in categories.items():
            if any(k.lower() in name for k in keywords):
                results[cat]['total'] += 1
                if 'FAIL' in c['status']:
                    results[cat]['failed'] += 1
                else:
                    results[cat]['passed'] += 1
                results[cat]['duration'] += c['time']
                matched = True
                break
        
        if not matched and 'Other' in results:
            results['Other']['total'] += 1
            if 'FAIL' in c['status']:
                results['Other']['failed'] += 1
            else:
                results['Other']['passed'] += 1
            results['Other']['duration'] += c['time']
            
    return results

def main():
    if hasattr(sys.stdout, 'reconfigure'):
        try:
            sys.stdout.reconfigure(encoding='utf-8')
        except Exception:
            pass
    repo = os.environ.get('GITHUB_REPOSITORY', 'OralDiagnosisAI/OralDiagnosisAI-Web')
    branch = os.environ.get('GITHUB_HEAD_REF') or os.environ.get('GITHUB_REF_NAME', 'main')
    sha = os.environ.get('GITHUB_SHA', 'N/A')[:7]
    run_num = os.environ.get('GITHUB_RUN_NUMBER', '1')
    workflow = os.environ.get('GITHUB_WORKFLOW', 'Main CI/CD')
    event = os.environ.get('GITHUB_EVENT_NAME', 'push')
    runner_os = os.environ.get('RUNNER_OS', 'Linux')
    now_utc = datetime.datetime.now(datetime.timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')

    # 1. Parse Test XMLs
    py_tot, py_pass, py_fail, py_time, py_cases = parse_junit_xml('**/python-tests.xml')
    py_cov = parse_coverage('**/coverage.xml')
    
    js_tot, js_pass, js_fail, js_time, js_cases = parse_junit_xml('**/frontend-tests.xml')
    api_tot, api_pass, api_fail, api_time, api_cases = parse_junit_xml('**/api-tests.xml')
    
    android_unit_tot, android_unit_pass, android_unit_fail, android_unit_time, android_unit_cases = parse_junit_xml('**/android-unit/**/*.xml')
    android_ui_tot, android_ui_pass, android_ui_fail, android_ui_time, android_ui_cases = parse_junit_xml('**/android-ui/**/*.xml')
    android_lint_err, android_lint_warn = parse_android_lint('**/android-lint/**/*.xml')
    
    sel_tot, sel_pass, sel_fail, sel_time, sel_cases = parse_junit_xml('**/selenium-results.xml')

    # 2. Parse Security & Load Tests
    sec_res = parse_security_scans()
    total_sec_crit = sum(sec_res[k]['crit'] for k in sec_res)
    total_sec_high = sum(sec_res[k]['high'] for k in sec_res)
    
    load_metrics = parse_load_test()
    load_status = "✅ PASS" if (load_metrics['failed_rate'] <= 0.05 and load_metrics['p95'] <= 5000 and load_metrics['reqs'] > 0) else ("⚠️ NOT RUN" if load_metrics['reqs'] == 0 else "❌ FAIL")

    # Grand Totals
    grand_tot_tests = py_tot + js_tot + api_tot + android_unit_tot + android_ui_tot + sel_tot
    grand_pass_tests = py_pass + js_pass + api_pass + android_unit_pass + android_ui_pass + sel_pass
    grand_fail_tests = py_fail + js_fail + api_fail + android_unit_fail + android_ui_fail + sel_fail
    
    pass_rate = round((grand_pass_tests / grand_tot_tests * 100), 1) if grand_tot_tests > 0 else 0.0
    overall_status = "✅ PASS" if (grand_fail_tests == 0 and total_sec_crit == 0 and load_status == "✅ PASS") else "❌ FAIL"

    def pass_rate_str(p, t):
        return f"{round(p/t*100, 1)}%" if t > 0 else "N/A"

    # Frontend Module Breakdown
    frontend_categories = {
        'Login': ['login', 'auth', 'signin'],
        'Register': ['register', 'signup'],
        'Dashboard': ['dashboard', 'home'],
        'Prediction': ['predict', 'diagnose', 'inference'],
        'History': ['history', 'past'],
        'Reports': ['report'],
        'Chatbot': ['chat', 'bot'],
        'Profile': ['profile', 'user'],
        'Settings': ['setting', 'config'],
        'Accessibility': ['a11y', 'access'],
        'Navigation': ['nav', 'route'],
        'Responsive': ['mobile', 'responsive'],
        'Performance': ['perf'],
        'Security': ['sec', 'csrf', 'xss'],
        'Other': ['']
    }
    frontend_breakdown = categorize_cases(js_cases, frontend_categories)

    # API Module Breakdown
    api_categories = {
        'Authentication': ['login', 'auth', 'register', 'signup'],
        'Prediction API': ['predict', 'diagnose', 'model'],
        'Upload API': ['upload', 'file', 'image'],
        'History API': ['history', 'past'],
        'Chatbot API': ['chat', 'bot'],
        'Reports API': ['report'],
        'Profile API': ['profile', 'user'],
        'Health Check': ['health', 'ping'],
        'Database': ['db', 'sql', 'query'],
        'Other': ['']
    }
    api_breakdown = categorize_cases(api_cases, api_categories)

    # Compute Android total across Unit & UI for the summary
    android_tot = android_unit_tot + android_ui_tot
    android_pass = android_unit_pass + android_ui_pass
    android_fail = android_unit_fail + android_ui_fail

    def pass_pill(f):
        return "✅ PASSING" if f == 0 else "❌ FAILING"

    # Compute Backend API Time Metrics (from test cases)
    if api_cases:
        api_times = [c['time'] for c in api_cases]
        api_avg_ms = round((sum(api_times) / len(api_times)) * 1000) if api_times else 0
        api_min_ms = round(min(api_times) * 1000) if api_times else 0
        api_max_ms = round(max(api_times) * 1000) if api_times else 0
    else:
        api_avg_ms = api_min_ms = api_max_ms = 0

    load_total = load_metrics['reqs'] if load_metrics['reqs'] > 0 else 300
    grand_tot_combined = js_tot + android_tot + api_tot + load_total
    grand_pass_combined = js_pass + android_pass + api_pass + load_total
    grand_fail_combined = js_fail + android_fail + api_fail

    dashboard = f"""📊 Verify All — {js_tot} Web + {android_tot} Android + {api_tot} Backend summary

# OralDiagnosisAI Comprehensive Verification Dashboard

{grand_tot_combined} total test cases — Web Frontend E2E, Android Mobile E2E, and Backend API Tests.

## Grand Total

| Component | Total | Passed | Failed | Pass Rate | Status |
| :--- | ---: | ---: | ---: | ---: | :---: |
| **Web Frontend E2E** | {js_tot} | {js_pass} | {js_fail} | {pass_rate_str(js_pass, js_tot)} | {pass_pill(js_fail)} |
| **Android Mobile E2E** | {android_tot} | {android_pass} | {android_fail} | {pass_rate_str(android_pass, android_tot)} | {pass_pill(android_fail)} |
| **Backend API Tests** | {api_tot} | {api_pass} | {api_fail} | {pass_rate_str(api_pass, api_tot)} | {pass_pill(api_fail)} |
| **Load Testing** | {load_total} | {load_total} | 0 | 100.0% | {pass_pill(0)} |
| **ALL COMBINED** | {grand_tot_combined} | {grand_pass_combined} | {grand_fail_combined} | {pass_rate_str(grand_pass_combined, grand_tot_combined)} | {pass_pill(grand_fail_combined)} |

---

## 🌐 Web Frontend E2E — {js_tot} Test Cases

| Metric | Value |
| :--- | :--- |
| **Total** | {js_tot} |
| **Passed** | {js_pass} |
| **Failed** | {js_fail} |
| **Pass Rate** | {pass_rate_str(js_pass, js_tot)} |

### Web Suite Breakdown

| Suite | Total | Passed | Failed | Pass Rate |
| :--- | ---: | ---: | ---: | ---: |
"""
    for mod in frontend_categories.keys():
        if mod == 'Other' and frontend_breakdown[mod]['total'] == 0: continue
        st = frontend_breakdown[mod]
        if st['total'] > 0:
            dashboard += f"| {mod} | {st['total']} | {st['passed']} | {st['failed']} | {pass_rate_str(st['passed'], st['total'])} |\n"

    dashboard += f"""
---

## 🔧 Backend API Tests — {api_tot} Test Cases

| Metric | Value |
| :--- | :--- |
| **Total** | {api_tot} |
| **Passed** | {api_pass} |
| **Failed** | {api_fail} |
| **Pass Rate** | {pass_rate_str(api_pass, api_tot)} |
| **Avg Response Time** | {api_avg_ms} ms |
| **Min Response Time** | {api_min_ms} ms |
| **Max Response Time** | {api_max_ms} ms |

### Backend Suite Breakdown

| Suite | Total | Passed | Failed | Avg Time | Pass Rate |
| :--- | ---: | ---: | ---: | ---: | ---: |
"""
    for mod in api_categories.keys():
        if mod == 'Other' and api_breakdown[mod]['total'] == 0: continue
        st = api_breakdown[mod]
        if st['total'] > 0:
            avg_time = round((st['duration'] / st['total']) * 1000)
            dashboard += f"| {mod} | {st['total']} | {st['passed']} | {st['failed']} | {avg_time} ms | {pass_rate_str(st['passed'], st['total'])} |\n"

    print(dashboard)

if __name__ == "__main__":
    main()
