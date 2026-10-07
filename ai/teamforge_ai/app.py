from typing import Annotated, Literal
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError
from fastapi import FastAPI, Query
from pydantic import BaseModel, Field, field_validator
from .demo import recommendations
from .demo import PROFILES
from .teams import recommend_team

app = FastAPI(title='TeamForge recommendations', docs_url=None, redoc_url=None)

@app.get('/profiles/samples')
def samples(offset: Annotated[int, Query(ge=0, le=800)] = 0):
    from .demo import sample_page
    return sample_page(offset)
class DescriptionRequest(BaseModel):
    description: Annotated[str, Field(min_length=1, max_length=1000)]

@app.post('/description/skills')
def description_skills(request: DescriptionRequest):
    from .description import suggest
    return suggest(request.description)

Text = Annotated[str, Field(min_length=1, max_length=80)]

class Profile(BaseModel):
    entityType: Literal['INDIVIDUAL', 'ORGANIZATION'] = 'INDIVIDUAL'
    matchingIntent: Literal['PROVIDER', 'SEEKER', 'COLLABORATOR'] = 'COLLABORATOR'
    description: Annotated[str, Field(max_length=1000)] = ''
    neededSkills: Annotated[list[Text], Field(max_length=20)] = []
    displayName: Annotated[str, Field(min_length=1, max_length=60)]
    role: Text
    skills: Annotated[list[Text], Field(min_length=1, max_length=20)]
    interests: Annotated[list[Text], Field(min_length=1, max_length=20)]
    rolesSought: Annotated[list[Text], Field(min_length=1, max_length=10)]
    weeklyHours: Annotated[int, Field(ge=1, le=60)]
    goal: Text
    workingStyle: Text
    timezone: Text
    availability: Annotated[list[Annotated[int, Field(ge=0, le=167)]], Field(min_length=1, max_length=168)]

    @field_validator('timezone')
    @classmethod
    def valid_timezone(cls, value):
        try: ZoneInfo(value)
        except (ZoneInfoNotFoundError, ValueError): raise ValueError('Invalid timezone')
        return value

@app.get('/health')
def health(): return {'status': 'ok', 'modelVersion': 'weighted-reciprocal-v2'}

@app.post('/recommendations/demo')
def demo(profile: Profile):
    return {'accountType': 'DEMO', 'syntheticProfileCount': len(PROFILES), 'recommendations': recommendations(profile.model_dump())}

class Project(BaseModel):
    name: Text
    description: Annotated[str, Field(min_length=1, max_length=1000)]
    domain: Text
    stage: Text
    technologies: Annotated[list[Text], Field(min_length=1, max_length=20)]
    rolesNeeded: Annotated[list[Text], Field(min_length=1, max_length=4)]
    weeklyHours: Annotated[int, Field(ge=1, le=60)]

    @field_validator('rolesNeeded')
    @classmethod
    def unique_roles(cls, value):
        if len(value) != len(set(value)): raise ValueError('Roles must be unique')
        return value

class TeamRequest(BaseModel):
    profile: Profile
    project: Project

@app.post('/teams/demo')
def demo_team(request: TeamRequest):
    return recommend_team(request.profile.model_dump(), request.project.model_dump(), PROFILES)


class RealCandidate(Profile):
    id: str
    accountType: Literal['REAL']

class RealRequest(BaseModel):
    profile: RealCandidate
    candidates: Annotated[list[RealCandidate], Field(max_length=200)]

@app.post('/recommendations/real')
def real_recommendations(request: RealRequest):
    from .ranking import rank
    ranked = rank(request.profile.model_dump(), [candidate.model_dump() for candidate in request.candidates], account_type='REAL')
    return {'accountType': 'REAL', 'recommendations': [item for item in ranked if item['compatibility'] > 50][:20]}
