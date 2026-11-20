import csv, io

def export_reports_to_csv(reports):
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(['ID', 'Patient', 'Diagnosis', 'Confidence', 'Date'])
    for r in reports:
        writer.writerow([r.get('id'), r.get('patient_name'), r.get('diagnosis'), r.get('confidence'), r.get('created_at')])
    return output.getvalue()
