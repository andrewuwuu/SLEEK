from typing import Optional, List
from pydantic import BaseModel, Field

class HealthData(BaseModel):
    age: int = Field(..., ge=0, description="Age of the user in years")
    gender: int = Field(..., ge=0, le=1, description="0 for male, 1 for female")
    height_cm: float = Field(..., gt=0, description="Height of the user in cm")
    weight_kg: float = Field(..., gt=0, description="Weight of the user in kg")
    food_allergies: Optional[List[str]] = Field(
        None, description="List of food allergies, if any"
    )

class UserPrediction(BaseModel):
    weight_category: str = Field(..., description="Weight category predicted")
    predicted_bmr: float = Field(..., gt=0, description="Predicted Basal Metabolic Rate")
