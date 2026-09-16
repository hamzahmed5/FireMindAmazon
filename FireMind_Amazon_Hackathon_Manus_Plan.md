# FireMind: Amazon Developer Hackathon Execution Plan

## Mission

Build and submit a **working Fire TV application** for the **Build,
Ship, Shape: Amazon Developer Hackathon**.

**Project:** FireMind\
**Track:** Fire TV\
**Platform:** Fire OS\
**Goal:** Ship a demo-ready TV app, public GitHub repo with open-source
license, demo video under 3 minutes on YouTube/Vimeo, screenshots,
Devpost description, and product feedback/friction log.

Do not submit a concept-only project. The final project must actually
run.

## 1. Product Concept

### Name

**FireMind**

### One-line description

> An AI-powered viewing companion that helps Fire TV users discover,
> understand, and interact with entertainment content.

### Elevator pitch

> Your intelligent AI companion for discovering and understanding what
> to watch on Fire TV.

### Problem

TV users face too many choices, slow remote-based search, generic
recommendations, and no simple way to ask natural-language questions
about what to watch.

### Solution

FireMind provides a TV-first AI assistant. Users can ask:

-   "I want something funny tonight."
-   "Recommend a short action movie."
-   "I want something similar to this."
-   "Give me three family-friendly options."
-   "Summarize this movie."
-   "Why should I watch this?"
-   "I have 90 minutes. What should I watch?"

## 2. Hackathon Requirements

Current official rules require Fire TV projects to:

-   Launch a demo-ready app working on **Fire OS or Vega OS**.
-   Use any suitable framework/language.
-   Show the project functioning on the intended platform in the demo
    video.
-   Provide a **public GitHub repository**.
-   Include source code, assets, setup/run instructions, and an
    open-source license.
-   Provide a demo video **under 3 minutes**.
-   Host the demo publicly on **YouTube or Vimeo**.
-   Provide project description and required submission fields.
-   Provide product feedback for each tool/API/SDK used.
-   Use third-party SDKs/APIs/data only under their applicable
    terms/licenses.
-   Submit original work.

Fire TV judging criteria are equally weighted:

1.  Tech Implementation
2.  Design
3.  Potential Impact
4.  Quality of the Idea

Avoid a basic streaming UI, basic video player, or simple
remote-controlled game. Target AI-enhanced viewing, multi-modal UX,
family entertainment, or another clearly useful Fire TV experience.

## 3. MVP Scope

Do not build Netflix. Do not build a full streaming service. Build a
focused, polished hackathon MVP.

### Screens

1.  Splash
2.  Home
3.  AI Assistant
4.  Recommendations
5.  Content Details
6.  Watchlist
7.  Settings/About

### Minimum flow

``` text
Launch
  -> Home
  -> Ask FireMind
  -> Enter/select request
  -> AI processes request
  -> Recommendation results
  -> Open content details
  -> Add to watchlist / choose another recommendation
```

## 4. Core Features

### AI Assistant

Natural-language request -\> structured recommendations.

Each result should contain:

-   Title
-   Genre
-   Year
-   Runtime
-   Short description
-   Why it matches
-   Optional rating/mood

### Mood Discovery

Quick choices:

-   Funny
-   Relaxing
-   Exciting
-   Family
-   Action
-   Mystery
-   Sci-Fi
-   Romantic
-   Short
-   Classic

### Smart Recommendations

Filters:

-   Genre
-   Mood
-   Runtime
-   Audience
-   Era
-   Favorite title

Return 3-5 recommendations, not huge lists.

### Content Details

``` text
TITLE
Year
Genre
Runtime
Rating
Description

Why FireMind recommends it:
<AI-generated explanation>

[ Add to Watchlist ]
[ Back ]
```

### Watchlist

Local persistence. Add/remove/open details. No account system required.

### Why This?

Every recommendation explains why it matches the request.

Example:

``` text
Why this?

You asked for:
- Sci-Fi
- Mind-bending
- Under 2 hours

FireMind selected this because it matches your preferences.
```

## 5. Optional Features

Only implement after MVP is stable.

### AI Summary

Short spoiler-safe summary on details page.

### Similar Content

``` text
Recommend something similar to this.
```

### Family Mode

Filter recommendations for family-friendly content.

### Multi-modal UX

Possible modes:

-   D-pad
-   On-screen text input
-   Voice input if reliable
-   Quick-select chips

Never sacrifice stable D-pad navigation for optional voice
functionality.

## 6. Recommended Stack

### Fire TV frontend

Recommended:

> Android + Kotlin

Reason: native Android approach, Fire TV compatibility, strong D-pad
control, straightforward packaging, reliable TV UI.

React Native is acceptable if it materially improves speed and Fire TV
compatibility is verified.

### Backend

Preferred AWS architecture if practical:

``` text
Fire TV
   -> API Gateway
   -> AWS Lambda
   -> Amazon Bedrock
   -> Structured JSON
   -> Fire TV
```

This can support the AWS Builder mini challenge. Do not add AWS only for
a label. The integration must actually run.

If Bedrock cannot be implemented reliably, use another authorized AI API
through a secure backend.

Never expose API keys in the Fire TV app.

## 7. Architecture

``` text
+----------------------+
|      Fire TV App     |
| Android / Kotlin     |
| TV-first UI          |
| D-pad navigation     |
+----------+-----------+
           |
           | HTTPS
           v
+----------------------+
| Backend API          |
| API Gateway/Lambda   |
+----------+-----------+
           |
           v
+----------------------+
| AI Provider          |
| Amazon Bedrock or    |
| authorized API       |
+----------+-----------+
           |
           v
     Structured JSON
```

## 8. Repository Structure

``` text
firemind/
├── app/
├── backend/
├── data/
├── assets/
├── docs/
├── README.md
├── LICENSE
└── .gitignore
```

Adapt to actual implementation. Do not create unnecessary files.

Suggested Android structure:

``` text
app/src/main/java/com/firemind/
├── MainActivity.kt
├── navigation/
├── ui/
│   ├── home/
│   ├── assistant/
│   ├── recommendations/
│   ├── details/
│   ├── watchlist/
│   └── settings/
├── data/
│   ├── model/
│   ├── repository/
│   └── local/
├── network/
└── ai/
```

## 9. TV UX Requirements

Every interactive element must have:

-   Clear focus state
-   Large focus target
-   Logical UP/DOWN/LEFT/RIGHT navigation
-   Visible selected state
-   Back navigation

Test:

``` text
UP
DOWN
LEFT
RIGHT
SELECT
BACK
```

No mouse dependency.

UI rules:

-   Large typography
-   Large controls
-   High contrast
-   TV-safe spacing
-   Minimal text density
-   Clear hierarchy
-   No tiny controls
-   No clutter
-   No desktop-style forms

## 10. Home Screen

Target layout:

``` text
+-----------------------------------------------------+
| FIREMIND                                  AI        |
|                                                     |
| What do you want to watch?                          |
|                                                     |
| [ Ask FireMind ]                                    |
|                                                     |
| Quick moods                                          |
| [Funny] [Action] [Family] [Sci-Fi] [Relaxing]       |
|                                                     |
| Recommended for you                                 |
| [Movie]    [Movie]    [Movie]    [Movie]            |
+-----------------------------------------------------+
```

## 11. Catalog

Use a small curated dataset: approximately 50-150 titles.

Example schema:

``` json
{
  "id": "movie_001",
  "title": "Example Movie",
  "year": 2020,
  "genre": ["Sci-Fi", "Drama"],
  "mood": ["Mind-bending", "Serious"],
  "runtime": 110,
  "familyFriendly": false,
  "rating": 8.1,
  "description": "..."
}
```

Use legally usable/public-domain/original metadata and assets. Do not
scrape copyrighted content illegally. Do not include copyrighted posters
unless usage rights permit them.

## 12. Recommendation Logic

Keep retrieval predictable. AI should rank/explain candidates rather
than inventing arbitrary movie metadata.

``` text
User request
  -> Extract preferences
  -> Filter local catalog
  -> Select candidates
  -> AI ranks/explains candidates
  -> Return top 3-5
```

Example:

``` text
User: "I want a funny movie under 100 minutes."

System:
  mood = funny
  runtime <= 100

Filter catalog
  -> candidates

AI
  -> rank + explain

UI
  -> top 3
```

## 13. AI Response Contract

Do not parse arbitrary AI prose in the frontend. Request structured
JSON.

``` json
{
  "recommendations": [
    {
      "title": "Example Movie",
      "year": 2020,
      "genre": "Sci-Fi",
      "runtime": 110,
      "reason": "Matches your request for a short mind-bending sci-fi movie.",
      "summary": "Short spoiler-safe description."
    }
  ]
}
```

Validate response. If AI fails, use a local fallback recommendation
path.

## 14. Backend API

Suggested endpoints:

``` text
GET  /api/health
POST /api/recommend
POST /api/summarize
POST /api/similar
```

Example:

``` http
POST /api/recommend
Content-Type: application/json
```

``` json
{
  "query": "I want something funny under two hours",
  "filters": {
    "runtimeMax": 120
  }
}
```

## 15. Security

Never hardcode credentials.

Never commit:

``` text
.env
local.properties
credentials.json
*.jks
*.keystore
API keys
AWS credentials
```

Search before publication:

``` text
API_KEY
SECRET
TOKEN
PASSWORD
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
OPENAI_API_KEY
GEMINI_API_KEY
```

Use environment variables/secrets management. Rotate any accidentally
exposed credential.

## 16. Development Setup

First inspect current Amazon Fire TV documentation. Do not blindly use
outdated versions.

Set up:

1.  Amazon Developer account.
2.  Android development environment if using Kotlin.
3.  Android SDK.
4.  Compatible JDK/Gradle versions.
5.  Fire TV/Vega simulator as appropriate.
6.  TV-compatible project configuration.
7.  Debug build.
8.  Install/run on simulator or device.

First prove a minimal Fire TV app launches before implementing FireMind.

## 17. Implementation Phases

### Phase 1: Repository

-   Create repository.
-   Initialize Git.
-   Create project structure.
-   Add `.gitignore`.
-   Add initial README.
-   Add LICENSE.

### Phase 2: Fire TV Shell

-   Create MainActivity.
-   Configure TV-compatible theme.
-   Create Home screen.
-   Implement D-pad focus.
-   Implement Back navigation.
-   Build and launch.

Acceptance:

``` text
App launches.
Home renders.
Remote navigation works.
Back works.
No crashes.
```

### Phase 3: Catalog

-   Create movie model.
-   Add dataset.
-   Add repository.
-   Add filtering.
-   Render cards.
-   Implement details screen.

Acceptance:

``` text
Catalog loads.
Cards render.
Details opens.
Filtering works.
```

### Phase 4: Watchlist

-   Add local persistence.
-   Add/remove titles.
-   Open details from watchlist.
-   Test after app restart.

Acceptance:

``` text
Add title.
Remove title.
Close app.
Reopen app.
Watchlist remains.
```

### Phase 5: Backend

Implement `/api/health` first, then `/api/recommend`, `/api/summarize`,
`/api/similar`.

Add:

-   Validation
-   Error handling
-   Timeout handling
-   Structured responses
-   Secure secrets

Acceptance:

``` text
Backend starts.
Health works.
Recommendation works.
Invalid request handled.
AI failure handled.
No secrets committed.
```

### Phase 6: Fire TV ↔ AI

Connect:

``` text
Fire TV
  -> HTTPS
  -> Backend
  -> AI
  -> JSON
  -> Fire TV
```

Show loading state. Handle network/AI errors. Never freeze the UI.

### Phase 7: AI Assistant UI

Create `Ask FireMind` screen with:

``` text
[ Text Input ]
[ Funny ]
[ Action ]
[ Family ]
[ Sci-Fi ]
[ Surprise Me ]
```

If remote typing is painful, prioritize quick prompts and keep text
input functional but simple.

### Phase 8: Recommendation Results

Show 3-5 cards. Each card shows:

``` text
Poster
Title
Year
Genre
Runtime
Why recommended
```

SELECT opens details.

### Phase 9: Details

Implement:

``` text
Title
Metadata
Description
AI reason
Summary
Watchlist button
Back
```

### Phase 10: Polish

Test:

-   Launch
-   Focus
-   Navigation
-   Back
-   Loading
-   Empty state
-   AI error
-   Network error
-   Long response
-   Missing image
-   Watchlist
-   App restart
-   Simulator restart

Remove debug text, placeholders, broken buttons, empty screens, fake
features, and unused controls.

## 18. Demo Flow

Use one strong story.

1.  Launch FireMind.
2.  Select `Ask FireMind`.
3.  Enter:

``` text
I want a mind-bending sci-fi movie under two hours.
```

4.  Show AI results.
5.  Open one title.
6.  Show `Why FireMind recommends it`.
7.  Show AI summary.
8.  Add to Watchlist.
9.  Return home.
10. Select `Family` or another quick mood.
11. Show new recommendations.

Target video length: 90-150 seconds. Absolute maximum: under 3 minutes.

## 19. Demo Script

Opening:

> FireMind is an AI-powered viewing companion built for Fire TV. Instead
> of scrolling through endless content, users can tell FireMind what
> they want to watch.

Interaction:

> I want a mind-bending sci-fi movie under two hours.

Results:

> FireMind understands the request and returns a small set of
> recommendations with an explanation for each choice.

Details:

> Each recommendation includes why it matches the request, plus a quick
> summary.

Watchlist:

> Users can save titles and return to them later.

Closing:

> FireMind turns Fire TV from a content browser into an intelligent
> viewing companion.

## 20. Video Requirements

Video must:

-   Be less than 3 minutes.
-   Be publicly visible.
-   Use YouTube or Vimeo.
-   Show the project functioning on Fire TV/simulator.
-   Avoid copyrighted music/material unless permission exists.
-   Be in English or provide required English translation/materials.

Record cleanly. Keep the app visible during actual interactions.

## 21. GitHub README

README must include:

``` text
# FireMind

Short description

## Features
## Architecture
## Tech Stack
## Requirements
## Setup
## Backend Setup
## AI Configuration
## Fire TV Build
## Fire TV Installation
## Running the App
## Demo
## Project Structure
## Environment Variables
## Troubleshooting
## Product Feedback / Friction Log
## License
```

Document actual commands, not guessed commands.

Example only:

``` bash
git clone <REPOSITORY_URL>
cd firemind
```

Windows Gradle example:

``` powershell
.\gradlew.bat assembleDebug
```

Use the actual build command and APK path generated by the project.

## 22. Git Workflow

Use meaningful commits:

``` text
feat: create Fire TV application shell
feat: add TV navigation
feat: add movie catalog
feat: add recommendation engine
feat: add AI backend
feat: connect Fire TV to AI backend
feat: add watchlist
feat: polish TV interface
docs: add setup instructions
docs: add friction log
```

Do not make one giant final commit.

## 23. Testing Checklist

### App

-   [ ] Builds
-   [ ] Installs
-   [ ] Launches
-   [ ] No startup crash
-   [ ] D-pad works
-   [ ] Back works
-   [ ] Focus visible
-   [ ] Home works
-   [ ] AI works
-   [ ] Recommendations work
-   [ ] Details work
-   [ ] Watchlist works
-   [ ] Loading state works
-   [ ] Error state works

### Backend

-   [ ] Health endpoint works
-   [ ] Recommendation endpoint works
-   [ ] AI works
-   [ ] Invalid requests handled
-   [ ] AI failure handled
-   [ ] Secrets protected
-   [ ] Demo endpoint accessible

### Fire TV

-   [ ] Tested on simulator/device
-   [ ] TV-safe UI
-   [ ] Remote navigation
-   [ ] Back navigation
-   [ ] Network works
-   [ ] App package installs

## 24. Friction Log

Record only real problems.

Template:

``` text
Date:
Tool:
Problem:
Expected:
Actual:
Error:
Impact:
Workaround:
Potential documentation improvement:
```

Example structure only. Do not fabricate an error.

Friction logs can provide up to a 10% judging bonus.

## 25. AWS Builder Mini Challenge

Optional but recommended only after primary Fire TV experience is
stable.

Use an actual AWS integration, for example:

``` text
Fire TV
  -> API Gateway
  -> Lambda
  -> Amazon Bedrock
```

Document actual:

-   AWS service used
-   Why it is used
-   Runtime flow
-   Configuration
-   Security
-   API calls

Do not claim AWS Builder without real runtime integration.

## 26. Open Source Mini Challenge

Optional. Primary Fire TV project comes first.

If attempted, create a qualifying contribution during the hackathon
period and retain:

-   Contribution URL
-   Repository URL
-   Description of contribution
-   Actual implementation

Follow exact current challenge rules.

## 27. Devpost Submission

### Project name

``` text
FireMind
```

### Elevator pitch

``` text
An AI-powered viewing companion that helps Fire TV users discover, understand, and interact with entertainment content.
```

### Project Story

Use this structure and rewrite to match the final implementation:

``` markdown
## Inspiration

Explain the TV discovery problem.

## What We Built

Explain FireMind.

## How It Works

Explain Fire TV -> backend -> AI -> recommendations.

## Key Features

List actual features.

## Built For Fire TV

Explain D-pad and TV-first interaction.

## What We Learned

Explain Fire TV, TV UX, and AI integration.

## Challenges

Explain real technical challenges.

## What's Next

Explain realistic future features.
```

Never claim a feature that was not implemented.

### Built With

Only include actual technologies. Possible final tags:

``` text
Amazon Fire TV
Fire OS
Android
Kotlin
Python
AWS
Amazon Bedrock
AWS Lambda
Amazon API Gateway
Generative AI
AI
LLM
Recommendation System
REST API
```

Remove unused tags.

### Try It Out

Add the actual public GitHub URL. Add demo URL only if real.

### Project Media

Upload clean screenshots:

1.  Home
2.  AI assistant
3.  AI request
4.  Recommendation results
5.  Content details
6.  Why This?
7.  Watchlist
8.  Fire TV simulator/device

No personal information. No credentials.

## 28. Product Feedback

For every actual tool/API/SDK used, document:

``` text
Which developer tools, APIs and SDKs did you use and for what?
```

Example only:

``` text
Amazon Fire TV:
Used to build and run the TV application.

Android/Kotlin:
Used to implement the Fire TV application and TV navigation.

Amazon Bedrock:
Used to generate AI-powered recommendation responses.

AWS Lambda:
Used as secure backend layer.

Amazon API Gateway:
Used to expose backend API endpoints.
```

Remove anything not actually used.

## 29. Final Security Audit

Before GitHub publication:

``` text
Search:
API_KEY
SECRET
TOKEN
PASSWORD
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
OPENAI_API_KEY
GEMINI_API_KEY
```

Check `.env`, `local.properties`, credentials, keystores, logs, and
configuration files.

## 30. Final Submission Checklist

### Project

-   [ ] FireMind runs
-   [ ] Fire TV platform verified
-   [ ] AI interaction works
-   [ ] Recommendations work
-   [ ] Details work
-   [ ] Watchlist works
-   [ ] D-pad works
-   [ ] Back works
-   [ ] Error handling works

### GitHub

-   [ ] Public repository
-   [ ] Source code
-   [ ] Assets
-   [ ] README
-   [ ] Setup instructions
-   [ ] Open-source license
-   [ ] No secrets
-   [ ] No broken links

### Demo

-   [ ] Video recorded
-   [ ] Under 3 minutes
-   [ ] Fire TV/simulator visible
-   [ ] Actual app functioning
-   [ ] YouTube/Vimeo URL works
-   [ ] No unauthorized copyrighted material

### Devpost

-   [ ] Project name
-   [ ] Elevator pitch
-   [ ] Project story
-   [ ] Built With
-   [ ] GitHub URL
-   [ ] Demo URL
-   [ ] Images
-   [ ] Video
-   [ ] Product feedback
-   [ ] Optional challenge fields if applicable

## 31. Manus Execution Rules

You are the implementation agent. Do not stop at planning.

For each task:

1.  Inspect current state.
2.  Implement.
3.  Build/run.
4.  Test.
5.  Fix errors.
6.  Continue.
7.  Document actual changes.

Do not ask for confirmation for normal implementation decisions.

Ask only when a decision requires information unavailable from the
repository/environment/account.

Do not fabricate successful results.

If a command fails:

``` text
1. Capture exact error.
2. Diagnose.
3. Fix.
4. Re-run.
5. Record real friction if relevant.
```

Never mark a task complete without verification.

## 32. Manus Priority Order

``` text
1. Fire TV app launches
2. Remote navigation works
3. Core UI works
4. Catalog works
5. Recommendation logic works
6. AI backend works
7. Fire TV <-> backend integration works
8. Watchlist works
9. Error handling works
10. Polish
11. GitHub README/license
12. Testing
13. Demo preparation
14. Devpost content
15. Product feedback
16. Optional AWS Builder
17. Optional Open Source challenge
```

If time becomes limited:

``` text
STOP optional features.
FINISH primary Fire TV experience.
```

## 33. Definition of Done

FireMind is DONE only when:

``` text
[ ] Fire TV app launches
[ ] Fire TV app is demo-ready
[ ] D-pad navigation works
[ ] AI assistant works
[ ] Recommendations work
[ ] Details work
[ ] Watchlist works
[ ] Backend works
[ ] Secrets protected
[ ] GitHub repository public
[ ] Open-source license present
[ ] README complete
[ ] Demo video < 3 minutes
[ ] Demo video publicly accessible
[ ] Screenshots uploaded
[ ] Devpost fields complete
[ ] Product feedback complete
[ ] Actual project matches submission
```

## 34. Final Product Principle

Build less. Polish more.

The strongest story is not:

> "We built a huge AI platform."

It is:

> "We built a polished AI-enhanced viewing experience specifically
> designed for Fire TV."

Make the interaction obvious within the first 10 seconds. Make the AI
useful. Make the TV experience excellent. Make the project actually run.
Then submit.

# 35. MANUS MASTER CAPABILITY / SKILL / TOOL REQUIREMENTS

This section is mandatory. Manus must treat the capability list below as the operating skill matrix for the entire FireMind project. If a capability is unavailable, Manus must identify the limitation, use the closest available tool, and continue. Do not pretend a tool was used when it was not.

## 35.1 Operating Mode

Manus must operate as a combined:

- Senior software engineer
- Android / Fire TV engineer
- AI engineer
- Backend engineer
- AWS engineer
- UI/UX designer
- TV interaction designer
- QA engineer
- Test automation engineer
- Security reviewer
- Performance engineer
- Accessibility reviewer
- DevOps / release engineer
- Git/GitHub maintainer
- Technical writer
- Hackathon compliance reviewer
- Demo/video producer
- Product manager
- Code reviewer
- Debugger
- Researcher

Manus must work autonomously through:

Inspect → Plan → Implement → Build → Run → Test → Debug → Re-test → Review → Document → Commit → Verify → Package → Submit.

Never stop after generating code. A task is incomplete until the result is actually verified.

## 35.2 Computer / Workspace Skills

Manus should be able to:

- Navigate the project workspace.
- Create, rename, move, copy, delete, and inspect files/directories.
- Read source files completely when needed.
- Search recursively through source code.
- Compare files and versions.
- Inspect file metadata.
- Extract ZIP/TAR archives.
- Create archives for release.
- Inspect images and screenshots.
- Capture screenshots where tooling permits.
- Inspect generated build artifacts.
- Monitor processes.
- Start/stop/restart development servers.
- Read stdout/stderr.
- Read application logs.
- Detect build/test failures automatically.
- Preserve existing folder structure unless there is a strong technical reason to change it.

## 35.3 Terminal / Shell Skills

Manus should be comfortable with:

- PowerShell
- Bash/sh when available
- Environment variables
- PATH configuration
- Process management
- Package installation
- Build commands
- Test commands
- File operations
- Port/process diagnostics
- Log inspection
- Piping and redirection
- Exit codes
- Re-running failed commands after fixing root cause

Never hide command failures. Record the failure, diagnose it, fix it, and rerun the command.

## 35.4 Web / Browser Research Skills

Manus must research official documentation when current behavior matters.

Priority sources:

1. Amazon official developer documentation
2. Amazon Fire TV / Fire OS / Vega documentation
3. AWS official documentation
4. Android official documentation
5. Kotlin official documentation
6. GitHub official documentation
7. Devpost official hackathon rules/resources
8. Official SDK/API repositories

Research tasks include:

- Verify current SDK/API names.
- Verify supported platforms.
- Verify Fire TV requirements.
- Verify simulator/device workflows.
- Verify AWS service behavior.
- Verify current dependency versions before locking them.
- Verify licensing requirements.
- Verify submission requirements.
- Verify APIs before implementing assumptions.

Never rely on random tutorials when an official source is available.

## 35.5 Git Skills

Manus must handle:

- git init
- git status
- git add
- git commit
- git diff
- git log
- git show
- git branch
- git switch/checkout
- git merge
- git stash
- git restore
- git remote
- git fetch
- git pull
- git push
- tags/releases where useful
- .gitignore
- GitHub repository setup
- README maintenance
- LICENSE creation
- release packaging
- commit history inspection
- secret scanning
- removal of accidental secrets from tracked files

Rules:

- Never commit API keys, tokens, passwords, credentials, local secrets, keystores, or private certificates.
- Use environment variables or safe configuration.
- Keep commits logical and descriptive.
- Inspect git diff before important commits.
- Confirm repository status after commits.
- Confirm pushed files actually exist remotely when possible.

## 35.6 GitHub Skills

Manus must be able to prepare the public repository for judges:

- Clear README
- Project description
- Features
- Architecture
- Tech stack
- Setup instructions
- Fire TV installation/run instructions
- Simulator instructions
- Backend setup
- Environment variables
- API configuration
- AI configuration
- Screenshots
- Demo information
- Testing instructions
- Known limitations
- License
- Contribution information if open-source challenge is used

Repository must not expose secrets or unnecessary personal data.

## 35.7 Android / Kotlin Skills

If Kotlin/Android is used, Manus must handle:

- Android Studio project structure
- Gradle
- Gradle Kotlin DSL
- Android SDK
- compileSdk / targetSdk / minSdk decisions
- AndroidManifest.xml
- Activities
- Fragments where useful
- lifecycle
- intents
- coroutines
- suspend functions
- ViewModel architecture
- state management
- networking
- JSON parsing
- HTTP clients
- serialization
- local storage
- SharedPreferences/DataStore where appropriate
- resources
- strings.xml
- colors/themes
- drawable assets
- fonts
- layouts or Compose where selected
- focus handling
- accessibility
- build variants
- debug/release builds
- APK generation
- dependency management
- dependency conflict resolution
- build cache issues
- Gradle daemon issues

## 35.8 Fire TV / TV Engineering Skills

FireMind is a TV product, not a phone app stretched onto a television.

Manus must understand and implement:

- Fire TV constraints
- Fire OS Android app behavior where applicable
- Vega OS considerations where applicable
- TV-safe UI
- D-pad navigation
- focus movement
- focus visibility
- Back button behavior
- remote interaction
- large-screen typography
- 10-foot UI principles
- readable spacing
- horizontal content rails
- cards
- TV-friendly dialogs
- loading states
- empty states
- error states
- network-loss states
- selection states
- accessibility
- performance on TV hardware

Every interactive element must be reachable using the remote without requiring a mouse or touch screen.

## 35.9 Fire TV Testing Skills

Manus must test on the strongest available target:

- Physical Fire TV device if available
- Fire TV simulator/emulator if available
- Compatible Android TV environment where useful for UI validation

Testing must include:

- cold launch
- warm launch
- app resume
- Back
- Home/re-entry
- D-pad up/down/left/right
- select/confirm
- focus transitions
- scrolling
- search
- AI interaction
- recommendation interaction
- details screen
- watchlist
- loading
- API failure
- AI failure
- timeout
- empty result
- malformed response
- slow network
- repeated actions
- rapid navigation

Use ADB where available:

- install APK
- uninstall APK
- launch app
- inspect package
- capture logcat
- filter logs
- diagnose crashes
- collect screenshots where supported

## 35.10 UI/UX Skills

Manus must design for:

- clarity
- hierarchy
- low interaction cost
- remote-first navigation
- visible focus
- consistent spacing
- readable typography
- strong contrast
- predictable navigation
- clear feedback
- minimal clutter
- polished empty/loading/error states

Avoid:

- mobile-first UI copied onto TV
- tiny text
- dense forms
- mouse-dependent controls
- excessive animations
- unnecessary screens
- AI-looking generic dashboard UI
- meaningless decorative components
- fake complexity

## 35.11 AI Engineering Skills

Manus must understand:

- LLM APIs
- multimodal models where useful
- prompt design
- structured JSON outputs
- schema validation
- retries
- timeouts
- fallbacks
- rate limits
- context limits
- token/cost awareness
- hallucination control
- grounding
- recommendation explanation
- prompt injection awareness
- input validation
- output validation

AI must have a clear product purpose.

For FireMind, AI should help with:

- natural-language viewing requests
- personalized recommendations
- content explanations
- summaries
- mood/intent interpretation
- follow-up questions

Do not add AI just for judging keywords.

## 35.12 Agentic AI Skills

If an agent workflow is used, Manus should understand:

- tools
- tool calling
- structured actions
- state
- context
- planning
- execution
- validation
- retry logic
- guardrails
- failure recovery
- deterministic tool results

Agent must not be allowed to perform arbitrary destructive actions.

Keep the AI workflow simple enough to demo reliably.

## 35.13 AWS Skills

If using the AWS Builder mini challenge, Manus must handle relevant AWS services such as:

- Amazon Bedrock
- Bedrock model invocation
- AWS SDKs
- IAM concepts
- Lambda
- API Gateway
- DynamoDB
- S3
- CloudWatch
- Secrets Manager / Parameter Store where appropriate
- AWS CLI
- environment configuration
- logging
- deployment
- permissions
- cost awareness

Only use services actually needed by the architecture.

Document:

- service used
- why it is used
- where it is used
- setup steps
- permissions needed
- environment variables
- cost considerations

Never hardcode AWS credentials.

## 35.14 Backend Skills

Manus must be capable of building or integrating a backend using an appropriate stack.

Capabilities:

- REST APIs
- JSON
- HTTP status codes
- request validation
- response validation
- authentication
- authorization
- sessions/tokens
- CORS where relevant
- error handling
- rate limiting where useful
- logging
- persistence
- environment variables
- API versioning where useful
- health checks

Backend must expose only required functionality.

## 35.15 API Contract Skills

Create and maintain an explicit API contract.

For each endpoint define:

- method
- path
- purpose
- authentication
- request body/query
- response body
- status codes
- errors
- example payload

Frontend must not invent backend endpoints.
Backend must not silently change response contracts.

## 35.16 Recommendation-System Skills

Manus must handle:

- content metadata
- user preferences
- genre matching
- semantic similarity where useful
- ranking
- recommendation scoring
- cold-start behavior
- deterministic fallback recommendations
- explanation generation
- duplicate removal
- result diversity

For the MVP, prefer reliable recommendation logic over an unnecessarily complex ML pipeline.

## 35.17 Data Skills

Manus must handle:

- JSON/CSV data
- schemas
- validation
- normalization
- missing values
- duplicate detection
- deterministic seed/demo data
- dataset documentation
- synthetic data labeling

Demo data must be clearly controlled and must not accidentally appear to be real user data.

## 35.18 Security Skills

Mandatory security review:

- secrets
- API keys
- credentials
- authentication
- authorization
- input validation
- output validation
- injection risks
- prompt injection
- unsafe URL handling
- file handling
- logging of sensitive information
- CORS
- dependency vulnerabilities
- debug mode
- production configuration

Before release search repository for likely secrets:

- API_KEY
- SECRET
- TOKEN
- PASSWORD
- ACCESS_KEY
- PRIVATE_KEY
- credential files
- .env files

Use placeholders in README, never real credentials.

## 35.19 Testing Skills

Manus must use multiple testing layers:

### Static

- compiler/build checks
- linting where configured
- type checking
- dependency checks
- secret scanning

### Unit

Test important deterministic functions.

### Integration

Test API + frontend/backend boundaries.

### UI

Test actual rendered screens.

### End-to-end

Test the complete user journey.

### Regression

After every important fix, rerun previously failing tests.

## 35.20 Automated Browser / UI Testing Skills

Where applicable, Manus may use:

- Playwright
- Android UI testing tools
- ADB automation
- emulator/simulator automation
- screenshot comparison

Test:

- navigation
- focus
- text
- buttons
- error states
- AI interactions
- loading
- data rendering

Do not treat a passing DOM test as proof of a good TV experience. Visual and remote interaction checks still matter.

## 35.21 Debugging Skills

Debug from evidence, not guesses.

Workflow:

1. Reproduce.
2. Capture exact error.
3. Identify failing layer.
4. Inspect relevant source.
5. Form root-cause hypothesis.
6. Make smallest appropriate fix.
7. Rebuild.
8. Re-run failing case.
9. Run regression checks.
10. Record result.

Never paper over errors with silent catches.

## 35.22 Performance Skills

Review:

- launch time
- network latency
- API calls
- unnecessary model calls
- image size
- memory
- UI rendering
- list/rail performance
- animations
- repeated recomposition/re-rendering
- caching

AI requests should not block the entire UI unnecessarily.

Provide useful loading feedback.

## 35.23 Accessibility Skills

Check:

- readable text
- contrast
- focus visibility
- focus order
- meaningful labels
- remote navigation
- non-color-only status communication
- reasonable text sizing
- reduced motion considerations where applicable

## 35.24 Asset / Visual Skills

Manus must manage:

- app icon
- logos
- thumbnails
- posters
- backgrounds
- AI-generated assets where used
- screenshots
- demo images

Assets must be legally usable and documented when required.

Do not use copyrighted assets in a way that creates an avoidable IP problem.

## 35.25 Video / Demo Skills

Manus must prepare a demo under 3 minutes.

Demo should show actual working project, preferably on:

- Fire TV
- Fire TV simulator
- Vega environment where applicable

Recommended sequence:

1. Launch.
2. Show home screen.
3. Navigate with remote.
4. Ask AI viewing question.
5. Show recommendation result.
6. Open title/details.
7. Demonstrate useful action such as watchlist.
8. Show one additional standout feature.
9. End with concise value proposition.

Do not spend most of the video on slides or talking.

## 35.26 FFmpeg / Media Tool Skills

Where available, Manus may use FFmpeg for:

- trimming
- joining clips
- converting formats
- compressing video
- extracting frames
- checking duration
- preparing demo assets

Final video must be:

- under 3 minutes
- publicly accessible
- easy for judges to view
- representative of actual functionality

## 35.27 Documentation Skills

Maintain documentation throughout development, not at the last minute.

Recommended files:

- README.md
- LICENSE
- API_SPEC.md
- ARCHITECTURE.md
- SETUP.md where useful
- TESTING.md where useful
- CHANGES.md
- FRICTION_LOG.md
- PRODUCT_FEEDBACK.md
- DEMO_SCRIPT.md
- .env.example

Do not create documentation files that add no value.

## 35.28 Hackathon Rules / Compliance Skills

Manus must continuously verify:

- Fire TV primary track requirements
- actual Fire TV/Fire OS/Vega demo requirement
- public GitHub requirement
- source code availability
- open-source license requirement
- demo video requirement
- YouTube/Vimeo accessibility
- project description
- screenshots/image gallery
- product feedback
- AWS Builder requirements if selected
- Open Source challenge requirements if selected
- submission deadline
- new/significantly updated project timing
- IP ownership

Never claim a mini challenge unless its requirements are actually satisfied.

## 35.29 Devpost Skills

Prepare every required submission field:

- project name
- elevator pitch
- project story
- built with
- try it out links
- image gallery
- video demo
- GitHub link
- track selection
- mini challenge selection
- product feedback
- AWS usage details where applicable
- Open Source details where applicable

Before submission compare Devpost content against actual repository and demo.

## 35.30 Product Feedback Skills

For each relevant Amazon developer tool/API/SDK, document:

- tool/API/SDK name
- what it was used for
- what worked well
- what needs improvement
- onboarding experience
- whether Manus would build with it again
- concrete examples

Avoid generic praise.

## 35.31 Friction Log Skills

Maintain a friction log whenever a real development obstacle occurs.

Each entry should contain:

- date
- tool/service
- task
- expected behavior
- actual behavior
- severity
- reproduction steps
- workaround
- impact
- actionable recommendation

Only report real friction encountered.

## 35.32 Open Source Skills

If entering the Open Source mini challenge:

- ensure qualifying work occurs during the allowed hackathon window
- maintain public repository history
- use a recognized open-source license
- document contribution/project details
- provide required URLs
- ensure new project or qualifying contribution satisfies the rules

Do not select Open Source simply because the repository is public.

## 35.33 Amazon Developer Tools / Knowledge Skills

When available, use official Amazon device development resources, including Amazon Devices Builder Tools and relevant MCP/agent skills.

Potential tasks include:

- Fire TV documentation lookup
- device capability lookup
- debugging guidance
- crash analysis
- performance analysis
- guided device workflows
- official API/documentation discovery

Use these tools when they materially improve accuracy.

## 35.34 Tool / Plugin Capability Matrix

Manus should use the best available tool for each task.

| Area | Preferred capability |
|---|---|
| Source code | Filesystem + editor |
| Shell | Terminal / PowerShell / Bash |
| Web research | Browser/web search |
| Official docs | Browser + documentation search |
| Git | Git CLI |
| GitHub | GitHub CLI/API/browser |
| Android | Android Studio + Gradle + SDK |
| Fire TV | Fire TV simulator/device tools |
| Device debugging | ADB + logcat |
| Backend | Runtime + API client |
| API testing | curl / Postman / equivalent |
| AI | Official model SDK/API |
| AWS | AWS CLI + AWS Console/docs |
| Data | Python / Node.js / suitable scripts |
| UI testing | Playwright / Android UI tools |
| Images | Image editor/generator as available |
| Video | FFmpeg + screen recording |
| Archives | ZIP/TAR tools |
| Documentation | Markdown/editor |
| Security | Secret scanning + dependency audit |

If a named tool is unavailable, do not fabricate its availability. Use an equivalent capability and document the substitution.

## 35.35 Python Skills

Use Python when useful for:

- data processing
- recommendation experiments
- validation
- scripts
- automated checks
- test data generation
- log analysis
- image inspection
- batch processing

Keep utility scripts purposeful. Do not add unnecessary dependencies.

## 35.36 Node.js / npm Skills

If the chosen stack uses JavaScript/TypeScript or tooling requires Node.js, Manus must handle:

- npm/pnpm/yarn as appropriate
- package.json
- dependency installation
- scripts
- build commands
- dev servers
- environment variables
- dependency updates
- lockfiles
- production builds

## 35.37 Networking Skills

Understand:

- DNS
- HTTPS
- HTTP methods
- status codes
- JSON
- timeouts
- retries
- connection failures
- TLS/certificate issues
- CORS
- localhost vs device networking
- emulator networking
- backend reachability from Fire TV

A backend working on a laptop is not automatically reachable from a TV device.

## 35.38 State Management Skills

Define clear states for:

- app startup
- loading
- success
- empty
- error
- retry
- AI thinking
- AI response
- network offline
- authentication
- watchlist
- recommendation refresh

Do not leave screens frozen during async work.

## 35.39 Reliability Skills

Manus must assume:

- network can fail
- AI can fail
- APIs can timeout
- model output can be malformed
- data can be missing
- users can press buttons rapidly
- users can press Back unexpectedly

Implement graceful recovery.

## 35.40 Quality Gate Before Any Major Milestone

Before saying "done", Manus must answer:

- Does it build?
- Does it launch?
- Does the main flow work?
- Does remote navigation work?
- Does AI work?
- Does recommendation work?
- Does error handling work?
- Are secrets protected?
- Is the UI polished?
- Is the repository clean?
- Is documentation current?
- Is the demo reproducible?

If any answer is no, continue working unless the item is explicitly marked as a known limitation.

# 36. MANUS AUTONOMOUS EXECUTION PROTOCOL

## Phase A: Inspect

1. Inspect workspace.
2. Identify existing project files.
3. Identify installed runtimes/tools.
4. Identify Android SDK availability.
5. Identify Fire TV simulator/device availability.
6. Inspect network/backend environment.
7. Inspect existing Git state.
8. Inspect relevant official documentation.
9. Confirm hackathon requirements.

## Phase B: Plan

Create a short implementation plan before major changes.

For every feature:

- goal
- files
- dependencies
- API contract
- test method
- acceptance criteria

## Phase C: Implement

Implement smallest complete vertical slice first:

Launch → Home → Navigate → AI request → Result → Details.

Then add:

Watchlist → recommendation polish → fallback states → visual polish → submission infrastructure.

## Phase D: Build

Run actual builds.

Fix:

- compiler errors
- Gradle errors
- dependency errors
- resource errors
- manifest errors
- runtime configuration errors

## Phase E: Test

Test every core journey manually and automatically where possible.

## Phase F: Debug

Use logs and reproduction evidence.

## Phase G: Review

Review:

- UX
- code quality
- security
- performance
- accessibility
- compliance

## Phase H: Package

Prepare:

- APK/build artifact
- README
- LICENSE
- screenshots
- demo video
- Devpost copy
- feedback
- friction log

## Phase I: Final Verification

Perform a clean-start test from the repository instructions.

A different person should be able to follow README instructions and understand how to run the project.

# 37. MANUS STOP CONDITIONS

Manus must NOT stop merely because:

- code was generated
- files were created
- build command was started
- a single test passed
- UI looks correct in source code
- API response exists

Manus may stop a task only when:

1. Acceptance criteria are satisfied.
2. Relevant tests pass.
3. Build succeeds.
4. Actual runtime behavior is verified.
5. Documentation is updated.
6. No known critical issue remains.
7. Any remaining limitation is explicitly recorded.

# 38. FIREMIND DEFINITION OF DONE

```text
[ ] Fire TV project builds
[ ] Fire TV app launches
[ ] App is demonstrable on Fire TV or required simulator
[ ] Remote D-pad navigation works
[ ] Focus states are obvious
[ ] Back behavior works
[ ] Home screen polished
[ ] Content browsing works
[ ] AI viewing assistant works
[ ] AI failures handled
[ ] Recommendations work
[ ] Recommendation fallback works
[ ] Details screen works
[ ] Watchlist works
[ ] Loading states work
[ ] Empty states work
[ ] Error states work
[ ] Backend/API works
[ ] API contract documented
[ ] Secrets protected
[ ] No credentials committed
[ ] Repository builds from clean checkout
[ ] Public GitHub repository ready
[ ] LICENSE present
[ ] README complete
[ ] Screenshots ready
[ ] Demo video under 3 minutes
[ ] Demo video publicly accessible
[ ] Devpost project fields complete
[ ] Product feedback complete
[ ] Friction log maintained if applicable
[ ] AWS Builder requirements satisfied if selected
[ ] Open Source requirements satisfied if selected
[ ] Actual project matches Devpost claims
[ ] Final security review complete
[ ] Final regression test complete
```

# 39. FINAL MANUS PRINCIPLE

Do not optimize for the amount of code produced.

Optimize for:

**working product + excellent TV UX + meaningful AI + reliable demo + clean repository + rule compliance.**

If forced to choose:

1. Working core flow
2. Fire TV UX quality
3. AI usefulness
4. Reliability
5. Security
6. Demo quality
7. Documentation
8. Extra features

Build less. Verify more. Polish what judges actually see.
