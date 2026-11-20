"""Pytest Global Test Fixtures and Synthetic Cytology Image Generators
"""

import io
import os
import sys
from pathlib import Path
import numpy as np
import pytest
from PIL import Image

ROOT_DIR = Path(__file__).resolve().parent.parent
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))

import app as app_module
from werkzeug.security import generate_password_hash


@pytest.fixture(autouse=True)
def setup_app_state():
    """Reset the application fallback database and set testing mode."""
    app_module.app.config.update(
        TESTING=True,
        WTF_CSRF_ENABLED=False,
    )
    app_module._reset_fallback_state()
    yield
    app_module._reset_fallback_state()


@pytest.fixture
def client():
    """Create Flask test client."""
    with app_module.app.test_client() as test_client:
        yield test_client


@pytest.fixture
def authenticated_client(client):
    """Create an authenticated Flask test client with an active session."""
    user_ref = app_module.users_ref.document("doctor_1")
    user_ref.set({
        "id": "doctor_1",
        "name": "Dr. Sarah Mitchell",
        "full_name": "Dr. Sarah Mitchell",
        "email": "doctor@hospital.org",
        "password_hash": generate_password_hash("DocPass123!"),
        "role": "Doctor",
    })
    with client.session_transaction() as sess:
        sess["user_id"] = "doctor_1"
        sess["user_name"] = "Dr. Sarah Mitchell"
    return client


@pytest.fixture
def synthetic_cytology_image():
    """Generate a synthetic H&E stained oral cytology image (224x224x3).

    Simulates microscopic eosin/hematoxylin staining with pink cytoplasm
    and purple nuclei that passes strict image validation.
    """
    np.random.seed(101)
    # Background pinkish cytoplasm (high red, moderate blue, low green)
    r = np.random.randint(180, 240, (224, 224), dtype=np.uint8)
    g = np.random.randint(60, 120, (224, 224), dtype=np.uint8)
    b = np.random.randint(140, 200, (224, 224), dtype=np.uint8)
    
    # Introduce cell nuclei (dark purple/blue circular clusters)
    for _ in range(12):
        cx, cy = np.random.randint(20, 200, 2)
        radius = np.random.randint(6, 14)
        y, x = np.ogrid[:224, :224]
        mask = (x - cx) ** 2 + (y - cy) ** 2 <= radius ** 2
        r[mask] = np.random.randint(40, 80)
        g[mask] = np.random.randint(20, 50)
        b[mask] = np.random.randint(80, 130)

    rgb = np.stack([r, g, b], axis=-1)
    img = Image.fromarray(rgb, mode="RGB")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    buf.seek(0)
    return buf


@pytest.fixture
def invalid_image():
    """Generate a non-cytology image (solid green landscape simulation)."""
    img = Image.new("RGB", (224, 224), color=(30, 220, 40))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    buf.seek(0)
    return buf
