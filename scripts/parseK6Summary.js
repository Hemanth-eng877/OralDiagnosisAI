const fs = require('fs');
const path = require('path');

// Defensive metric extraction to support various k6 summary schemas
function getMetricValue(metrics, metricName, property) {
    if (!metrics || !metrics[metricName]) return 'N/A';
    
    const metric = metrics[metricName];
    if (metric.values && metric.values[property] !== undefined) {
        return metric.values[property];
    }
    if (metric[property] !== undefined) {
        return metric[property];
    }
    return 'N/A';
}

function formatDuration(value) {
    if (value === 'N/A') return 'N/A';
    return `${parseFloat(value).toFixed(2)} ms`;
}

function formatRate(value) {
    if (value === 'N/A') return 'N/A';
    return `${(parseFloat(value) * 100).toFixed(2)}%`;
}

function formatBytes(value) {
    if (value === 'N/A') return 'N/A';
    const mb = parseFloat(value) / (1024 * 1024);
    return `${mb.toFixed(2)} MB`;
}

function parseSummary() {
    console.log('Parsing k6 summary.json...');
    const summaryPath = path.join(process.cwd(), 'summary.json');
    
    if (!fs.existsSync(summaryPath)) {
        console.error('Error: summary.json not found!');
        process.exit(1);
    }

    let summaryData;
    try {
        summaryData = JSON.parse(fs.readFileSync(summaryPath, 'utf8'));
    } catch (e) {
        console.error('Error: Failed to parse summary.json', e.message);
        process.exit(1);
    }

    const metrics = summaryData.metrics || {};

    // Extract metrics defensively
    const totalRequests = getMetricValue(metrics, 'http_reqs', 'count');
    const rps = getMetricValue(metrics, 'http_reqs', 'rate');
    const avgResponseTime = getMetricValue(metrics, 'http_req_duration', 'avg');
    const minResponseTime = getMetricValue(metrics, 'http_req_duration', 'min');
    const maxResponseTime = getMetricValue(metrics, 'http_req_duration', 'max');
    const medResponseTime = getMetricValue(metrics, 'http_req_duration', 'med');
    const p90ResponseTime = getMetricValue(metrics, 'http_req_duration', 'p(90)');
    const p95ResponseTime = getMetricValue(metrics, 'http_req_duration', 'p(95)');
    
    const failRate = getMetricValue(metrics, 'http_req_failed', 'rate');
    const checkPassRate = getMetricValue(metrics, 'checks', 'rate');
    
    const dataSent = getMetricValue(metrics, 'data_sent', 'count');
    const dataRecv = getMetricValue(metrics, 'data_received', 'count');

    // Formatters specific for the new table layout
    const fmtMs = (val) => val === 'N/A' ? 'N/A' : `${parseFloat(val).toFixed(0)} ms`;
    const fmtRate = (val) => val === 'N/A' ? 'N/A' : `${(parseFloat(val) * 100).toFixed(2)}%`;
    const fmtRateOne = (val) => val === 'N/A' ? 'N/A' : `${(parseFloat(val) * 100).toFixed(1)}%`;
    
    // Evaluate Result (against basic thresholds)
    const p95Limit = 1500;
    const avgLimit = 800;
    const failRateLimit = 0.05;
    
    let overallResult = 'PASS';
    let p95Status = '✅ PASS';
    let avgStatus = '✅ PASS';
    let errStatus = '✅ PASS';
    
    if (failRate !== 'N/A' && parseFloat(failRate) >= failRateLimit) { overallResult = 'FAIL'; errStatus = '❌ FAIL'; }
    if (p95ResponseTime !== 'N/A' && parseFloat(p95ResponseTime) >= p95Limit) { overallResult = 'FAIL'; p95Status = '❌ FAIL'; }
    if (avgResponseTime !== 'N/A' && parseFloat(avgResponseTime) >= avgLimit) { overallResult = 'FAIL'; avgStatus = '❌ FAIL'; }

    const overallStatusHtml = overallResult === 'PASS' ? '🟢 PASSED' : '🔴 FAILED';
    
    // Generate Markdown Report exactly matching the requested format
    const reportMd = `
## ⚡ OralDiagnosisAI App Load Testing — Baseline (100 VUs x 1 Min)

100 Virtual Users running for 1 minute against the application.

### 🎯 Overall Result: ${overallStatusHtml}

| Metric | Value |
| :--- | :--- |
| **Total Requests** | ${totalRequests} |
| **Requests / Second** | ${rps === 'N/A' ? 'N/A' : parseFloat(rps).toFixed(1)} req/s |
| **Avg Response Time** | ${fmtMs(avgResponseTime)} |
| **Min Response Time** | ${fmtMs(minResponseTime)} |
| **p95 Response Time** | ${fmtMs(p95ResponseTime)} |
| **Max Response Time** | ${fmtMs(maxResponseTime)} |
| **HTTP Error Rate** | ${fmtRate(failRate)} |
| **Check Pass Rate** | ${fmtRateOne(checkPassRate)} |

### ✅ Threshold Validation

| Threshold | Limit | Actual | Status |
| :--- | :--- | :--- | :--- |
| **p95 Response Time** | < 1,500 ms | ${fmtMs(p95ResponseTime)} | ${p95Status} |
| **Avg Response Time** | < 800 ms | ${fmtMs(avgResponseTime)} | ${avgStatus} |
| **HTTP Error Rate** | < 5% | ${fmtRate(failRate)} | ${errStatus} |
| **Check Pass Rate** | > 85% | ${fmtRateOne(checkPassRate)} | ${checkPassRate === 'N/A' || parseFloat(checkPassRate) > 0.85 ? '✅ PASS' : '❌ FAIL'} |

<details>
<summary><b>Load Test Cases (Scenarios)</b></summary>
Primary endpoints: Health, Login, Register, Predict, Profile, Dashboard, History.
</details>

### 📖 What the Numbers Mean

| Metric | Your Result | Interpretation |
| :--- | :--- | :--- |
| **Requests per second** | ${rps === 'N/A' ? 'N/A' : parseFloat(rps).toFixed(1)} req/s | Site handled ~${rps === 'N/A' ? 0 : Math.round(parseFloat(rps))} requests/sec |
| **Average response** | ${fmtMs(avgResponseTime)} | Typical user waits ${fmtMs(avgResponseTime)} |
| **Fastest response** | ${fmtMs(minResponseTime)} | Best-case latency |
| **Slowest response** | ${fmtMs(maxResponseTime)} | Worst-case latency |
| **p95 response** | ${fmtMs(p95ResponseTime)} | 95% of users under ${fmtMs(p95ResponseTime)} |

*Generated by OralDiagnosisAI CI/CD — k6 Load Testing Pipeline*
`;

    fs.writeFileSync('performance-summary.md', reportMd.trim());
    console.log(reportMd.trim());

    // Append to GITHUB_STEP_SUMMARY if available
    const stepSummaryPath = process.env.GITHUB_STEP_SUMMARY;
    if (stepSummaryPath) {
        const buildInfo = `### Build Information\n- **Build Number**: ${process.env.GITHUB_RUN_NUMBER || 'Local'}\n- **Commit SHA**: ${process.env.GITHUB_SHA || 'N/A'}\n\n`;
        fs.appendFileSync(stepSummaryPath, buildInfo + reportMd + '\n');
    }
}

parseSummary();
