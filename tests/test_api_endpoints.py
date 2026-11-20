import io
import json
import sys
from pathlib import Path
import pytest
from PIL import Image

ROOT_DIR = Path(__file__).resolve().parent.parent
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))

import app as app_module

@pytest.fixture(autouse=True)
def reset_state():
    app_module._reset_fallback_state()
    app_module.app.config.update(TESTING=True)
    yield
    app_module._reset_fallback_state()

@pytest.fixture()
def client():
    with app_module.app.test_client() as client:
        yield client

def test_api_signup_success(client):
    response = client.post(
        "/api/signup",
        json={"name": "Dr. Android", "email": "android@example.com", "password": "SecurePass123"},
    )
    assert response.status_code == 200
    data = json.loads(response.data)
    assert data["status"] == "success"
    assert "user_id" in data

def test_api_login_success(client):
    client.post(
        "/api/signup",
        json={"name": "Dr. Android", "email": "android@example.com", "password": "SecurePass123"},
    )
    response = client.post(
        "/api/login",
        json={"email": "android@example.com", "password": "SecurePass123"},
    )
    assert response.status_code == 200
    data = json.loads(response.data)
    assert data["status"] == "success"
    assert data["email"] == "android@example.com"

def test_api_login_invalid_credentials(client):
    response = client.post(
        "/api/login",
        json={"email": "nonexistent@example.com", "password": "WrongPassword"},
    )
    assert response.status_code == 401
    data = json.loads(response.data)
    assert data["status"] == "error"

def test_api_dashboard_stats(client):
    response = client.get("/api/dashboard?user_id=test_user_1")
    assert response.status_code == 200
    data = json.loads(response.data)
    assert data["status"] == "success"
    assert "patient_count" in data
    assert "report_count" in data

def test_api_patient_crud_lifecycle(client):
    # Add patient
    add_resp = client.post(
        "/api/patients/add",
        json={
            "user_id": "test_user_1",
            "full_name": "API Test Patient",
            "age": 35,
            "gender": "Female",
            "phone": "5550001111",
            "email": "apitest@example.com",
            "notes": "Routine check"
        }
    )
    assert add_resp.status_code == 200
    add_data = json.loads(add_resp.data)
    assert add_data["status"] == "success"

    # Get patients list
    get_resp = client.get("/api/patients?user_id=test_user_1")
    assert get_resp.status_code == 200
    get_data = json.loads(get_resp.data)
    assert get_data["status"] == "success"
    assert len(get_data["patients"]) > 0
    patient_id = get_data["patients"][0]["id"]

    # Edit patient
    edit_resp = client.put(
        f"/api/patients/{patient_id}/edit",
        json={
            "user_id": "test_user_1",
            "full_name": "API Test Patient Updated",
            "age": 36
        }
    )
    assert edit_resp.status_code == 200
    edit_data = json.loads(edit_resp.data)
    assert edit_data["status"] == "success"

    # Delete patient
    delete_resp = client.delete(f"/api/patients/{patient_id}/delete?user_id=test_user_1")
    assert delete_resp.status_code == 200
    delete_data = json.loads(delete_resp.data)
    assert delete_data["status"] == "success"

def test_api_reports_list(client):
    response = client.get("/api/reports?user_id=test_user_1")
    assert response.status_code == 200
    data = json.loads(response.data)
    assert data["status"] == "success"
    assert isinstance(data["reports"], list)

def test_api_diagnose_endpoint(client, monkeypatch, tmp_path):
    app_module.patients_ref.document("p_123").set({
        "id": "p_123",
        "user_id": "test_user_1",
        "full_name": "Test Patient"
    })
    monkeypatch.setattr(app_module, "predict_image", lambda path: ("Normal", 98.5))
    image_path = tmp_path / "api_scan.png"
    Image.new("RGB", (224, 224), color=(100, 200, 100)).save(image_path)
    
    with image_path.open("rb") as handle:
        response = client.post(
            "/api/diagnose",
            data={
                "user_id": "test_user_1",
                "patient_id": "p_123",
                "image": (io.BytesIO(handle.read()), "api_scan.png")
            },
            content_type="multipart/form-data"
        )
    assert response.status_code == 200
    data = json.loads(response.data)
    assert data["status"] == "success"
    assert data["diagnosis"] == "Normal"
    assert data["confidence"] == 98.5


def test_api_health_liveness(client):
    response = client.get("/api/health")
    assert response.status_code == 200
    data = json.loads(response.data)
    assert data["status"] == "healthy"
    assert "timestamp" in data


def test_api_health_readiness(client):
    response = client.get("/api/health/ready")
    assert response.status_code in (200, 503)
    data = json.loads(response.data)
    assert "checks" in data
    assert "database" in data["checks"]


def test_api_system_status(client):
    response = client.get("/api/system/status")
    assert response.status_code == 200
    data = json.loads(response.data)
    assert data["app_name"] == "OralDiagnosisAI"
    assert "version" in data
    assert "class_labels" in data
