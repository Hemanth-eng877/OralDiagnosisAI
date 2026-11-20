// Live preview for uploaded histology slides
function previewCytologySlide(input, imgElementId) {
  if (input.files && input.files[0]) {
    const reader = new FileReader();
    reader.onload = e => document.getElementById(imgElementId).src = e.target.result;
    reader.readAsDataURL(input.files[0]);
  }
}
