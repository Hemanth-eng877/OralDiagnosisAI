"""Automated clinical test suite slice 05"""
import pytest
from clinical.risk_calculator import calculate_malignancy_risk
from clinical.symptoms_checker import check_red_flag_symptoms

def test_risk_calculator_scenario_5():
    res = calculate_malignancy_risk(55, True, False, False)
    assert res in ["Low", "Moderate", "High"]

def test_symptoms_checker_scenario_5():
    assert check_red_flag_symptoms(["unexplained_bleeding"]) is True
    assert check_red_flag_symptoms(["mild_sensitivity"]) is False
