"""Unit tests for cytology image validation and preprocessing pipeline.
"""

import io
import os
import numpy as np
import pytest
from PIL import Image

import app as app_module
from app import is_valid_microscopic_image, preprocess_image, allowed_file


def test_allowed_file_matrix():
    assert allowed_file("slide.png") is True
    assert allowed_file("sample.jpg") is True
    assert allowed_file("sample.JPEG") is True
    assert allowed_file("scan.webp") is True
    assert allowed_file("malicious.exe") is False
    assert allowed_file("script.sh") is False
    assert allowed_file("document.pdf") is False
    assert allowed_file("no_extension") is False


def test_microscopic_image_validation_dimensions(tmp_path):
    app_module.app.config["TESTING"] = False
    try:
        small_img = Image.new("RGB", (30, 30), color=(200, 100, 150))
        img_path = str(tmp_path / "too_small.png")
        small_img.save(img_path)
        assert is_valid_microscopic_image(img_path) is False
    finally:
        app_module.app.config["TESTING"] = True


def test_microscopic_image_validation_blank_image(tmp_path):
    app_module.app.config["TESTING"] = False
    try:
        blank_img = Image.new("RGB", (224, 224), color=(200, 200, 200))
        img_path = str(tmp_path / "blank.png")
        blank_img.save(img_path)
        assert is_valid_microscopic_image(img_path) is False
    finally:
        app_module.app.config["TESTING"] = True


def test_microscopic_image_validation_green_dominant(tmp_path):
    app_module.app.config["TESTING"] = False
    try:
        green_img = np.random.randint(0, 100, (224, 224, 3), dtype=np.uint8)
        green_img[:, :, 1] = 250  # Strong green channel
        img = Image.fromarray(green_img)
        img_path = str(tmp_path / "green.png")
        img.save(img_path)
        assert is_valid_microscopic_image(img_path) is False
    finally:
        app_module.app.config["TESTING"] = True


def test_microscopic_image_validation_valid_he_stain(tmp_path):
    app_module.app.config["TESTING"] = False
    try:
        # Generate H&E stained pattern (pink/purple)
        r = np.random.randint(180, 240, (224, 224), dtype=np.uint8)
        g = np.random.randint(60, 120, (224, 224), dtype=np.uint8)
        b = np.random.randint(140, 200, (224, 224), dtype=np.uint8)
        rgb = np.stack([r, g, b], axis=-1)
        img = Image.fromarray(rgb)
        img_path = str(tmp_path / "valid_he.png")
        img.save(img_path)
        assert is_valid_microscopic_image(img_path) is True
    finally:
        app_module.app.config["TESTING"] = True


def test_preprocess_image_output_contract(tmp_path):
    r = np.random.randint(180, 240, (224, 224), dtype=np.uint8)
    g = np.random.randint(60, 120, (224, 224), dtype=np.uint8)
    b = np.random.randint(140, 200, (224, 224), dtype=np.uint8)
    img = Image.fromarray(np.stack([r, g, b], axis=-1))
    img_path = str(tmp_path / "he_sample.png")
    img.save(img_path)

    tensor = preprocess_image(img_path)
    assert tensor.shape == (1, 224, 224, 3)
    assert tensor.dtype == np.float32
    assert np.min(tensor) >= 0.0
    assert np.max(tensor) <= 1.0


def test_preprocess_image_corrupted_file(tmp_path):
    bad_file = tmp_path / "corrupted.png"
    bad_file.write_text("not an image")
    with pytest.raises(ValueError, match="valid microscopic oral cytology image"):
        preprocess_image(str(bad_file))
