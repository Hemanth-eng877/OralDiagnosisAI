// Trigger download of clinical CSV reports
function downloadCSV(csvContent, filename) {
  const blob = new Blob([csvContent], { type: 'text/csv' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename; a.click();
}
