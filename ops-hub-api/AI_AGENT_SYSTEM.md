# AI Agent Permission and Guardrail System

## Overview

The Ops Hub AI Agent system is designed with strict permission checks and guardrails to ensure AI actions respect RBAC rules and never bypass security controls. The AI agent acts as a **privileged assistant** that operates within the user's permission boundaries, not as an admin.

## Key Principles

1. **AI is Context-Aware**: AI knows the logged-in user, their roles, permissions, and current page/module
2. **Permission-Based Actions**: AI can only perform actions allowed by user permissions
3. **Confirmation Required**: Restricted actions require explicit user confirmation
4. **Comprehensive Logging**: All AI actions are logged in `ai_actions` and `audit_logs`
5. **No RBAC Bypass**: AI cannot bypass RBAC rules - it operates with user's permissions

## Architecture

### Components

1. **AiAgentService** (`com.company.ops_hub_api.service.AiAgentService`)
   - Core service for AI interactions
   - Handles conversations and action execution
   - Enforces permission checks
   - Manages confirmation tokens for restricted actions

2. **AiActionValidator** (`com.company.ops_hub_api.service.AiActionValidator`)
   - Validates AI actions against user permissions
   - Determines if actions require confirmation
   - Maps action types to required permissions

3. **AiAgentController** (`com.company.ops_hub_api.controller.AiAgentController`)
   - REST endpoints for AI interactions
   - Protected with `@RequiresPermission("USE_AI_AGENT")`

4. **Repositories**
   - `AiConversationRepository`: Manages AI conversations
   - `AiActionRepository`: Manages AI actions

## Permission Requirements

### Base Permission
- **USE_AI_AGENT**: Required to use the AI agent at all

### Action-Specific Permissions
AI actions require specific permissions based on the action type:

| Action Type | Required Permission |
|------------|---------------------|
| VIEW_CUSTOMERS | VIEW_CUSTOMERS |
| ASSIGN_CUSTOMERS | ASSIGN_CUSTOMERS |
| MANAGE_CUSTOMERS | MANAGE_CUSTOMERS |
| DELETE_CUSTOMER | MANAGE_CUSTOMERS |
| REASSIGN_CUSTOMER | ASSIGN_CUSTOMERS |
| COLLECT_PAYMENT | COLLECT_PAYMENT |
| VIEW_PAYMENTS | VIEW_PAYMENTS |
| APPROVE_PROFILE | APPROVE_PROFILE |
| REJECT_PROFILE | APPROVE_PROFILE |
| VIEW_REPORTS | VIEW_REPORTS |
| EXPORT_REPORTS | EXPORT_REPORTS |
| DATA_QUERY | VIEW_REPORTS |
| REPORT_GENERATION | VIEW_REPORTS |
| MANAGE_USERS | MANAGE_USERS |
| MANAGE_SETTINGS | MANAGE_SETTINGS |

## Restricted Actions

The following actions **always require confirmation**:

- DELETE_CUSTOMER
- DELETE_USER
- REASSIGN_CUSTOMER
- APPROVE_PROFILE
- REJECT_PROFILE
- EXPORT_REPORTS
- MANAGE_USERS
- MANAGE_SETTINGS

## AI Context

The AI agent is aware of:

- **User Information**: User ID, employee ID, username, full name
- **Roles**: All roles assigned to the user
- **Permissions**: All permissions granted to the user
- **Current Page**: Current page/module the user is on
- **Additional Context**: Any additional context data provided

This context is passed to the AI in every interaction, ensuring the AI understands the user's capabilities and limitations.

## Workflow

### 1. User Sends Message

```http
POST /api/ai/message
Authorization: Bearer <token>
Content-Type: application/json

{
  "message": "Show me all customers",
  "currentPage": "/customers",
  "currentModule": "customer-management"
}
```

**Response:**
```json
{
  "conversationId": "uuid",
  "response": "I can help you view customers...",
  "suggestedActions": [
    {
      "actionType": "DATA_QUERY",
      "actionName": "VIEW_CUSTOMERS",
      "description": "View customer information",
      "requiresPermission": true,
      "requiredPermission": "VIEW_CUSTOMERS",
      "requiresConfirmation": false
    }
  ],
  "requiresConfirmation": false,
  "context": {
    "userId": 1,
    "employeeId": "EMP001",
    "roles": ["MANAGER"],
    "permissions": ["VIEW_CUSTOMERS", "USE_AI_AGENT"]
  }
}
```

### 2. Execute Action (No Confirmation Required)

```http
POST /api/ai/action
Authorization: Bearer <token>
Content-Type: application/json

{
  "actionType": "DATA_QUERY",
  "actionName": "VIEW_CUSTOMERS",
  "conversationId": "uuid",
  "actionData": {}
}
```

**Response:**
```json
{
  "actionId": 123,
  "actionType": "DATA_QUERY",
  "actionName": "VIEW_CUSTOMERS",
  "executionStatus": "COMPLETED",
  "resultData": { ... },
  "requiresConfirmation": false
}
```

### 3. Execute Restricted Action (Confirmation Required)

```http
POST /api/ai/action
Authorization: Bearer <token>
Content-Type: application/json

{
  "actionType": "DELETE",
  "actionName": "DELETE_CUSTOMER",
  "conversationId": "uuid",
  "actionData": { "customerId": 456 }
}
```

**First Response (Confirmation Required):**
```json
{
  "actionType": "DELETE",
  "actionName": "DELETE_CUSTOMER",
  "executionStatus": "PENDING",
  "requiresConfirmation": true,
  "confirmationToken": "confirmation-token-uuid"
}
```

**Second Request (With Confirmation Token):**
```http
POST /api/ai/action
Authorization: Bearer <token>
Content-Type: application/json

{
  "actionType": "DELETE",
  "actionName": "DELETE_CUSTOMER",
  "conversationId": "uuid",
  "actionData": { "customerId": 456 },
  "confirmationToken": "confirmation-token-uuid"
}
```

**Response:**
```json
{
  "actionId": 124,
  "actionType": "DELETE",
  "actionName": "DELETE_CUSTOMER",
  "executionStatus": "COMPLETED",
  "resultData": { ... },
  "requiresConfirmation": false
}
```

## Permission Checks

### 1. AI Agent Access Check
```java
if (!userPrincipal.hasPermission("USE_AI_AGENT")) {
    throw new AccessDeniedException("Insufficient permission to use AI agent");
}
```

### 2. Action Permission Check
```java
actionValidator.validateActionPermission(actionType, actionName);
```

### 3. Confirmation Check
```java
boolean requiresConfirmation = actionValidator.requiresConfirmation(actionType, actionName);
```

## Logging

### AI Conversations
All AI conversations are logged to:
- `ai_conversations` table
- `audit_logs` table (action: `AI_CONVERSATION`)

### AI Actions
All AI actions are logged to:
- `ai_actions` table
- `audit_logs` table (action: `AI_ACTION_EXECUTED`, `AI_ACTION_DENIED`, `AI_ACTION_FAILED`)

### Audit Log Fields
- `actionType`: Type of action (AI_CONVERSATION, AI_ACTION_EXECUTED, etc.)
- `entityType`: Entity type (AI_CONVERSATION, AI_ACTION)
- `entityId`: ID of the conversation or action
- `oldValues`: Previous state (null for new actions)
- `newValues`: Action data and results
- `triggeredBy`: Always "AI_AGENT" for AI-triggered actions

## Security Features

### 1. Permission Enforcement
- AI cannot perform actions the user doesn't have permission for
- Permission checks happen before action execution
- Access denied errors are logged

### 2. Confirmation Tokens
- Restricted actions require confirmation tokens
- Tokens are one-time use and expire after 5 minutes
- Tokens are validated before action execution

### 3. Context Isolation
- AI conversations are user-specific
- Users can only access their own conversations
- Conversation access is verified before returning data

### 4. Audit Trail
- All AI actions are logged with full context
- Failed actions are logged with error messages
- Permission denials are logged separately

## API Endpoints

### Get AI Context
```http
GET /api/ai/context?currentPage=/customers&currentModule=customer-management
Authorization: Bearer <token>
```

### Process AI Message
```http
POST /api/ai/message
Authorization: Bearer <token>
Content-Type: application/json

{
  "message": "user message",
  "conversationId": "optional",
  "currentPage": "optional",
  "currentModule": "optional",
  "context": {}
}
```

### Execute AI Action
```http
POST /api/ai/action
Authorization: Bearer <token>
Content-Type: application/json

{
  "actionType": "DATA_QUERY",
  "actionName": "VIEW_CUSTOMERS",
  "conversationId": "optional",
  "actionData": {},
  "confirmationToken": "required for restricted actions"
}
```

### Get User Conversations
```http
GET /api/ai/conversations
Authorization: Bearer <token>
```

### Get Conversation Actions
```http
GET /api/ai/conversations/{conversationId}/actions
Authorization: Bearer <token>
```

## Integration with LLM

The `AiAgentService.processMessage()` method is designed to integrate with any LLM service:

```java
// TODO: Integrate with actual AI/LLM service
String aiResponse = generateAiResponse(request.getMessage(), context);
```

Replace this with your LLM integration (OpenAI, Anthropic, etc.).

## Best Practices

1. **Always Check Permissions**: Never bypass permission checks, even for AI
2. **Require Confirmation**: For destructive or sensitive actions
3. **Log Everything**: All AI actions should be logged for audit
4. **Validate Context**: Always validate user context before processing
5. **Handle Errors Gracefully**: Don't expose sensitive information in error messages
6. **Rate Limiting**: Consider rate limiting for AI endpoints
7. **Token Expiration**: Confirmation tokens should expire quickly
8. **One-Time Tokens**: Confirmation tokens should be single-use

## Testing

### Test Permission Enforcement
```java
// User without USE_AI_AGENT permission
// Should get 403 Forbidden

// User without VIEW_CUSTOMERS trying to view customers
// Should get 403 Forbidden
```

### Test Confirmation Flow
```java
// Request restricted action without token
// Should get confirmation token

// Request with invalid token
// Should get 403 Forbidden

// Request with valid token
// Should execute action
```

## Future Enhancements

- [ ] LLM integration (OpenAI, Anthropic, etc.)
- [ ] Action delegation to specific services
- [ ] Conversation history management
- [ ] AI action templates
- [ ] Multi-language support
- [ ] AI action scheduling
- [ ] AI action rollback
- [ ] Redis-based confirmation tokens (for distributed systems)
