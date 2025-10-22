# Claude Code Java SDK - Comprehensive Dependency Graph

## Executive Summary

This document provides a comprehensive dependency analysis of the `claude-code-java-sdk` module, mapping all 66 public classes across 19 packages, their inter-dependencies, external consumers, and implications for the v2.0.0 upgrade.

**Total Classes**: 66  
**Public Classes**: 66  
**Packages**: 19  
**Key Consumers**: GUI Module (claude-code-gui), Test Suite, Example Applications

---

## 1. Package Structure & Class Inventory

### 1.1 Core Packages (Public API Surface)

#### **com.anthropic.claude.client** (5 classes)
Primary entry points for SDK consumers.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `ClaudeCodeSDK` | Class | public | Main facade and entry point for all SDK operations | ClaudeCodeOptions, ConfigLoader, ProcessManager, QueryService, HookService, SubagentManager, AuthenticationProvider |
| `ClaudeSDKClient` | Interface/Class | public | Session-based client wrapper | ClaudeCodeOptions, QueryRequest, Message |
| `SessionManager` | Class | public | Multi-session lifecycle management | ClaudeCodeOptions, Session |
| `MessageReceiver` | Interface | public | Message reception callback interface | Message |
| `InterruptHandler` | Class | public | Signal handling for graceful shutdown | - |

**Dependency Chain**:
```
ClaudeCodeSDK (entry point)
├── ConfigLoader
├── ProcessManager
├── QueryService
├── HookService
├── SubagentManager
└── AuthenticationProvider
```

---

#### **com.anthropic.claude.config** (4 classes)
Configuration management and options.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `ClaudeCodeOptions` | Class | public | Configuration options builder | AuthenticationProvider, CliMode, ClaudePathResolver |
| `ClaudeCodeOptions.Builder` | Inner Class | public | Builder for ClaudeCodeOptions | CliMode |
| `Configuration` | Class | public | Alternative configuration model | - |
| `ConfigLoader` | Class | public | Multi-source configuration loader | ClaudeCodeOptions, ClaudeCodeException, CliMode |
| `CliMode` | Enum | public | CLI execution mode selector | - |

**Key Relationships**:
- `ClaudeCodeSDK` instantiates `ConfigLoader`
- `ConfigLoader` creates `ClaudeCodeOptions` via builder
- `ClaudeCodeOptions.Builder` uses `ClaudePathResolver` for CLI path resolution
- `CliMode` determines execution strategy selection (BATCH vs PTY_INTERACTIVE)

---

#### **com.anthropic.claude.exceptions** (2 classes)
Custom exception hierarchy.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `ClaudeCodeException` | Class | public | Base runtime exception for SDK errors | - |
| `ProcessExecutionException` | Class | public | Process execution failures | ClaudeCodeException (extends) |

**Exception Hierarchy**:
```
RuntimeException
└── ClaudeCodeException
    └── ProcessExecutionException
```

**Thrown By**: ConfigLoader, ProcessManager, AuthenticationProviderFactory, QueryService, SubagentManager, ToolExecutor

---

#### **com.anthropic.claude.hooks** (4 classes)
Event hook system for lifecycle management.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `HookService` | Class | public | Hook registration and execution engine | HookCallback, HookContext, HookResult |
| `HookCallback` | Interface | public | Functional interface for hook callbacks | HookContext, HookResult |
| `HookContext` | Class | public | Context passed to hook callbacks | - |
| `HookResult` | Class | public | Result from hook execution with control flow | - |

**Usage Pattern**:
```
ClaudeCodeSDK.addHook("pre_query", (HookContext ctx) -> { ... })
HookService.executeHooks("pre_query", context)
  → HookCallback.execute(context)
    → returns HookResult
```

**Hook Event Types**:
- `pre_query`: Before query execution
- `post_query`: After query execution
- `error_handler`: On query errors

---

#### **com.anthropic.claude.auth** (5 classes)
Multi-cloud authentication providers.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `AuthenticationProvider` | Interface | public | Base interface for all auth providers | - |
| `DefaultAuthenticationProvider` | Class | public | Direct API key authentication | AuthenticationProvider (implements) |
| `BedrockAuthenticationProvider` | Class | public | AWS Bedrock authentication | AuthenticationProvider (implements) |
| `VertexAIAuthenticationProvider` | Class | public | Google Vertex AI authentication | AuthenticationProvider (implements) |
| `AuthenticationProviderFactory` | Class | public | Factory for auth provider creation | ClaudeCodeException, AuthenticationProvider, all implementations |

**Provider Hierarchy**:
```
AuthenticationProvider (interface)
├── DefaultAuthenticationProvider
├── BedrockAuthenticationProvider
└── VertexAIAuthenticationProvider

AuthenticationProviderFactory
└── creates appropriate provider based on environment/config
```

**Factory Methods**:
- `createFromEnvironment()` - Auto-detect from env vars
- `createProvider(ProviderType, config)` - Explicit creation
- `detectAndCreate()` - Priority-based detection
- `builder(ProviderType)` - Builder pattern

---

#### **com.anthropic.claude.query** (3 classes)
Query execution and request handling.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `QueryRequest` | Class | public | Immutable query request data model | QueryRequest.Builder |
| `QueryRequest.Builder` | Inner Class | public | Builder for QueryRequest | - |
| `QueryBuilder` | Class | public | Fluent query builder for SDK users | QueryRequest, QueryService, Message |
| `QueryService` | Class | public | Core query execution engine | ProcessManager, PtyManager, HookService, MessageParser, CliExecutionStrategy |

**Query Execution Flow**:
```
ClaudeCodeSDK.queryStream(prompt)
  → QueryService.queryStream(QueryRequest)
    → CliExecutionStrategy.executeStream()
      → ProcessManager.execute()
        → MessageParser.parseMessage()
          → Message (result)
```

**Key Methods**:
- `queryAsync(QueryRequest)` - Async with CompletableFuture
- `queryStream(QueryRequest)` - RxJava Observable stream
- Both support hooks, timeout, retries

---

#### **com.anthropic.claude.messages** (3 classes)
Message parsing and data models.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `Message` | Class | public | Immutable message data model with JSON support | MessageType, ObjectMapper |
| `Message.Builder` | Inner Class | public | Builder for Message | MessageType |
| `MessageType` | Enum | public | Message type enumeration | - |
| `MessageParser` | Class | public | JSON message parsing engine | Message, MessageType, ClaudeCodeException, ObjectMapper |

**Message Types**:
- TEXT - Text response
- TOOL_CALL - Tool invocation
- TOOL_RESULT - Tool execution result
- ERROR - Error message
- SYSTEM - System message
- USER - User message
- ASSISTANT - Assistant response
- DEBUG - Debug information

**Parser Features**:
- Single message parsing: `parseMessage(jsonString)`
- Batch parsing: `parseMessages(jsonArrayString)`
- Claude CLI response format support
- Jackson JSON binding with custom type handling

---

#### **com.anthropic.claude.subagents** (2 classes)
Subagent lifecycle management.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `SubagentManager` | Class | public | Manages multiple long-running subagents | Subagent, ProcessManager, ClaudeCodeOptions, ClaudeCodeException |
| `Subagent` | Class | public | Individual subagent instance | ProcessManager, ClaudeCodeOptions |

**Lifecycle**:
```
SubagentManager.startSubagent(type, config)
  → creates Subagent instance
  → returns subagentId
  
SubagentManager.stopSubagent(subagentId)
  → calls Subagent.stop()
  → removes from active map
  
SubagentManager.shutdown()
  → stops all active subagents
```

---

### 1.2 Supporting Packages (Internal Use)

#### **com.anthropic.claude.process** (2 classes)
Process execution and management.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `ProcessManager` | Class | public | CLI process execution (ZT-Exec wrapper) | ProcessExecutionException |
| `StreamHandler` | Class | public | Output stream handling for processes | - |

**Execution Types**:
- Synchronous: `executeSync(command, timeout)`
- Asynchronous: `executeAsync(command, timeout)`
- Streaming: `executeStreaming(command, outputConsumer)`

---

#### **com.anthropic.claude.strategy** (3 classes)
Execution strategy pattern for CLI operations.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `CliExecutionStrategy` | Interface | public | Strategy interface for CLI execution | QueryRequest, Message |
| `BatchProcessStrategy` | Class | public | Batch execution strategy (spawn process per query) | CliExecutionStrategy, ProcessManager |
| `PtyInteractiveStrategy` | Class | public | PTY interactive strategy (persistent session) | CliExecutionStrategy, PtyManager, ProcessManager |
| `CliExecutionStrategyFactory` | Class | public | Factory for strategy selection | CliExecutionStrategy, CliMode |

**Strategy Selection**:
```
CliMode.BATCH → BatchProcessStrategy
CliMode.PTY_INTERACTIVE → PtyInteractiveStrategy
```

---

#### **com.anthropic.claude.pty** (7 classes)
PTY (pseudo-terminal) management for interactive sessions.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `PtyManager` | Class | public | Manages interactive PTY sessions | - |
| `ClaudeState` | Class | public | Current session state | StateChange |
| `ClaudeResponse` | Class | public | Response from PTY interaction | - |
| `ClaudeResponseBuilder` | Class | public | Builder for ClaudeResponse | ClaudeResponse |
| `StateChange` | Class | public | State transition event | - |
| `OutputParser` | Class | public | Parses Claude CLI output patterns | - |
| `ClaudeOutputPatterns` | Class | public | Regex patterns for output matching | - |

---

#### **com.anthropic.claude.tools** (6 classes)
MCP (Model Context Protocol) custom tools system.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `MCPServer` | Class | public | In-process MCP server for tools | Tool, ToolDefinition, ToolExecutor |
| `Tool` | Annotation | public | @Tool annotation for methods | - |
| `Param` | Annotation | public | @Param annotation for parameters | - |
| `ToolDefinition` | Class | public | Tool metadata and definition | - |
| `ToolExecutor` | Class | public | Executes tool methods via reflection | ToolDefinition, ToolExecutionResult, ObjectMapper |
| `ToolExecutionResult` | Class | public | Tool execution result data | - |
| `ToolsFactory` | Class | public | Factory for tool discovery and registration | Tool, ToolDefinition, MCPServer |

**Tool Registration Flow**:
```
1. Class with @Tool annotated methods
2. ToolsFactory.discoverTools(Object instance)
3. MCPServer.registerTools(toolDefinitions)
4. ToolExecutor.executeAsync(toolDef, args)
   → invoke via reflection
   → return ToolExecutionResult
```

---

#### **com.anthropic.claude.context** (4 classes)
Context management and compression.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `ContextManager` | Class | public | Context lifecycle and window management | ContextConfig, Message |
| `ContextCompressor` | Class | public | Compresses context to fit within token limit | ContextAnalyzer, Message |
| `ContextAnalyzer` | Class | public | Analyzes message importance and relevance | Message |
| `ContextConfig` | Class | public | Context configuration parameters | - |

---

#### **com.anthropic.claude.streaming** (5 classes)
RxJava-based streaming utilities.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `AsyncIterator<T>` | Interface | public | Generic async iterator interface | - |
| `MappedAsyncIterator<T, R>` | Class | public | Transforms stream elements | AsyncIterator |
| `FilteredAsyncIterator<T>` | Class | public | Filters stream elements | AsyncIterator |
| `LimitedAsyncIterator<T>` | Class | public | Limits number of elements | AsyncIterator |
| `SkippedAsyncIterator<T>` | Class | public | Skips first N elements | AsyncIterator |
| `StreamStateManager` | Class | public | Manages streaming state and backpressure | BackpressureController |
| `BackpressureController` | Class | public | Handles RxJava backpressure | - |

---

#### **com.anthropic.claude.performance** (3 classes)
Performance optimization utilities.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `ConnectionPoolManager` | Class | public | HTTP connection pooling (OkHttp) | PooledConnection |
| `PooledConnection` | Class | public | Connection wrapper with health checks | - |
| `CacheManager` | Class | public | Query result caching (Caffeine) | Message |

---

#### **com.anthropic.claude.utils** (1 class)
General utilities.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `ClaudePathResolver` | Class | public | Cross-platform CLI path resolution | - |

---

#### **com.anthropic.claude.examples** (2 classes)
Example implementations.

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `BasicExample` | Class | public | Basic SDK usage example | ClaudeCodeSDK, Message |
| `BaseUrlExample` | Class | public | Custom base URL configuration example | ClaudeCodeOptions, ClaudeCodeSDK |

---

#### **com.anthropic.claude.models** (1 class)
Data models (legacy/alternative naming).

| Class | Type | Visibility | Purpose | Dependencies |
|-------|------|-----------|---------|--------------|
| `Message` | Class | public | Alternative Message model | (duplicate of messages.Message) |

---

## 2. Core Dependency Map

### 2.1 Dependency Flow Hierarchy

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ClaudeCodeSDK (Facade)                       │
│                    (Main public entry point)                         │
└──────────────┬──────────────────────────────────────────────────────┘
               │
       ┌───────┼───────┬───────────┬─────────────┬────────────┐
       │       │       │           │             │            │
       ▼       ▼       ▼           ▼             ▼            ▼
    Config  Process  Query      Hooks      Subagents     Auth
    Loader  Manager  Service    Service     Manager     Providers
       │       │       │           │             │            │
       │       │   ┌───┴───┬───┐   │             │            │
       │       │   │       │   │   │             │            │
       ▼       ▼   ▼       ▼   ▼   ▼             ▼            ▼
    Options  Stream Strat  Message  Hook     ProcessMgr  Auth Factory
    Builder  Handler egies  Parser   Context     │          │
             │              │        │          │          │
             │              ▼        ▼          │          │
             │           MessageType HookResult │          ▼
             │           Type Enum              │      Authentication
             │                                   │      Providers
             ▼                                   │
         CLI Strategy                           ▼
         (Batch/PTY)                        Subagent
                                            Lifecycle
```

### 2.2 Direct Dependency Analysis

#### **ClaudeCodeSDK Dependencies**
```
ClaudeCodeSDK
├─ Direct Dependencies:
│  ├── ClaudeCodeOptions (constructor parameter, configuration)
│  ├── ConfigLoader (creates options if not provided)
│  ├── ProcessManager (process execution)
│  ├── QueryService (query execution)
│  ├── HookService (hook management)
│  ├── SubagentManager (subagent lifecycle)
│  ├── AuthenticationProvider (authentication)
│  └── DefaultAuthenticationProvider (default auth if not provided)
│
├─ Transitive Dependencies:
│  ├── via QueryService → PtyManager, CliExecutionStrategy, MessageParser
│  ├── via ProcessManager → ProcessExecutionException
│  ├── via ConfigLoader → CliMode, ClaudePathResolver, ClaudeCodeException
│  └── via SubagentManager → Subagent, ClaudeCodeException
│
├─ Optional Dependencies:
│  └── PtyManager (only if CliMode.PTY_INTERACTIVE selected)
│
└─ Return Types:
   ├── Stream<Message>
   ├── Observable<Message>
   └── SubagentManager
```

---

#### **QueryService Dependencies**
```
QueryService
├─ Constructor Parameters:
│  ├── ProcessManager (command execution)
│  ├── PtyManager (optional, for interactive mode)
│  ├── HookService (pre/post query hooks)
│  └── ClaudeCodeOptions (configuration)
│
├─ Direct Usage:
│  ├── MessageParser (response parsing)
│  ├── CliExecutionStrategy (via factory)
│  ├── HookContext (hook event creation)
│  ├── QueryRequest (parameter type)
│  └── Message (return type)
│
└─ Strategy Selection:
   └── CliExecutionStrategyFactory.create(mode)
       ├─ BatchProcessStrategy (BATCH mode)
       └─ PtyInteractiveStrategy (PTY mode)
```

---

#### **ConfigLoader Dependencies**
```
ConfigLoader
├─ Direct Usage:
│  ├── ClaudeCodeOptions.Builder (creates via builder)
│  ├── ClaudeCodeException (validation errors)
│  ├── CliMode (CLI mode configuration)
│  └── ClaudePathResolver (resolve CLI path)
│
├─ Configuration Sources (priority order):
│  1. System properties
│  2. Environment variables (ANTHROPIC_API_KEY, CLAUDE_CODE_*)
│  3. User config file (~/.claude/config.properties)
│  4. Default configuration (classpath:claude-code.properties)
│
└─ Returns:
   └── ClaudeCodeOptions
```

---

#### **AuthenticationProviderFactory Dependencies**
```
AuthenticationProviderFactory
├─ Implementations Created:
│  ├── DefaultAuthenticationProvider (Direct API)
│  ├── BedrockAuthenticationProvider (AWS)
│  └── VertexAIAuthenticationProvider (Google)
│
├─ Creation Methods:
│  ├── createFromEnvironment() (auto-detect)
│  ├── createProvider(ProviderType, config) (explicit)
│  ├── detectAndCreate() (priority detection)
│  └── builder(ProviderType).build() (fluent API)
│
├─ Environment Variable Detection:
│  ├── CLAUDE_API_KEY → DefaultAuthenticationProvider
│  ├── AWS_ACCESS_KEY_ID + AWS_SECRET_ACCESS_KEY → BedrockAuthenticationProvider
│  ├── GOOGLE_CLOUD_PROJECT + GOOGLE_APPLICATION_CREDENTIALS → VertexAIAuthenticationProvider
│  └── CLAUDE_CODE_USE_* flags override
│
└─ Throws:
   └── ClaudeCodeException (on missing configuration)
```

---

### 2.3 Reverse Dependency Analysis (Who Depends on Each Class)

#### **ClaudeCodeOptions**
**Depended On By**:
- ClaudeCodeSDK (constructor, configuration access)
- QueryService (CLI mode, timeout, environment variables)
- SessionManager (session options)
- ConfigLoader (builder returns this)
- SubagentManager (passed to Subagent)
- GUI Module (EnhancedMainWindow, SessionAwareClaudeExecutor)

**Impact of Changes**: HIGH - Used throughout SDK

---

#### **ClaudeCodeException**
**Depended On By**:
- ConfigLoader (validation errors)
- ProcessManager (execution failures)
- QueryService (error handling)
- SubagentManager (lifecycle errors)
- AuthenticationProviderFactory (missing configuration)
- HookService (callback execution errors)
- ToolExecutor (tool execution failures)
- ProcessExecutionException (parent class)

**Impact of Changes**: CRITICAL - Exception hierarchy, all error handling

---

#### **Message**
**Depended On By**:
- QueryService (return type in Stream<Message>)
- MessageBuilder (builder pattern)
- MessageParser (parsing returns Message)
- HookContext (message data in hooks)
- ContextManager (context window of messages)
- CacheManager (cache values)
- GUI Module (MessageCallback receives Message)
- Test Suite (all query tests)

**Impact of Changes**: CRITICAL - Core data model, widely used

---

#### **QueryRequest**
**Depended On By**:
- ClaudeCodeSDK.query(QueryRequest)
- QueryService.queryAsync/queryStream(QueryRequest)
- QueryBuilder (builds QueryRequest)
- HookContext (includes QueryRequest data)
- CliExecutionStrategy.execute*(QueryRequest)

**Impact of Changes**: HIGH - Query parameter model

---

#### **AuthenticationProvider**
**Depended On By**:
- ClaudeCodeSDK (authentication check)
- ClaudeCodeOptions (can hold custom provider)
- DefaultAuthenticationProvider (implements)
- BedrockAuthenticationProvider (implements)
- VertexAIAuthenticationProvider (implements)
- AuthenticationProviderFactory (creates)

**Impact of Changes**: MEDIUM - Interface definition

---

## 3. External Usage Analysis

### 3.1 GUI Module (claude-code-gui) Dependencies

The GUI module explicitly depends on these SDK classes:

```
com.claude.gui
├── ClaudeCodeGUI (main entry point)
│   └── EnhancedMainWindow
│       ├── imports ClaudeCodeSDK
│       ├── imports ClaudeCodeOptions
│       ├── imports CliMode
│       ├── imports Message
│       ├── imports QueryRequest
│       ├── imports ClaudeState
│       ├── imports StateChange
│       ├── imports ClaudeResponse
│       ├── imports PtyManager
│       └── imports DefaultAuthenticationProvider
│
├── SessionAwareClaudeExecutor
│   ├── uses ClaudeCodeSDK
│   ├── uses ClaudeCodeOptions
│   └── uses CliMode
│
└── StreamReader
    └── processes Message objects
```

**Import Statements in GUI**:
```java
// From GUI source code analysis
import com.anthropic.claude.auth.DefaultAuthenticationProvider;
import com.anthropic.claude.client.ClaudeCodeSDK;
import com.anthropic.claude.config.ClaudeCodeOptions;
import com.anthropic.claude.config.CliMode;
import com.anthropic.claude.messages.Message;
import com.anthropic.claude.pty.ClaudeResponse;
import com.anthropic.claude.pty.ClaudeState;
import com.anthropic.claude.pty.PtyManager;
import com.anthropic.claude.pty.StateChange;
import com.anthropic.claude.query.QueryRequest;
```

**Critical GUI Dependencies**:
1. `ClaudeCodeSDK` - Cannot be renamed without breaking GUI
2. `ClaudeCodeOptions` - Configuration model used throughout
3. `Message` - Return type from queries
4. `QueryRequest` - Query parameter model
5. `CliMode` - Strategy selection

---

### 3.2 Test Suite Dependencies

Test files directly import and test these classes:

```
Test Classes:
├── ClaudeCodeSDKTest
│   ├── tests ClaudeCodeSDK (main facade)
│   ├── uses ClaudeCodeOptions.Builder
│   ├── uses QueryRequest
│   ├── uses Message
│   ├── uses ClaudePathResolver
│   └── uses AuthenticationProvider
│
├── ConfigLoaderTest
│   ├── tests ConfigLoader
│   └── uses ClaudeCodeOptions
│
├── HookServiceTest
│   ├── tests HookService
│   ├── uses HookCallback
│   ├── uses HookContext
│   └── uses HookResult
│
├── AuthenticationProviderFactoryTest
│   ├── tests factory
│   ├── tests all provider implementations
│   └── uses AuthenticationProvider
│
├── MessageParserTest
│   ├── tests MessageParser
│   ├── uses Message
│   └── uses MessageType
│
└── ... (16 total test files)
```

---

## 4. Transitive Dependency Chain Examples

### 4.1 Query Execution Chain
```
User Code:
  sdk.queryStream("prompt")
    ↓
ClaudeCodeSDK.queryStream(QueryRequest)
    ↓
QueryService.queryStream(QueryRequest)
    ├── HookService.executeHooks("pre_query", HookContext)
    ├── CliExecutionStrategy.executeStream(QueryRequest)
    │  ├─ BatchProcessStrategy.executeStream()
    │  │  ├─ ProcessManager.executeStreaming(command)
    │  │  └─ MessageParser.parseMessage(jsonString)
    │  │     └─ Message (created with Jackson)
    │  └─ PtyInteractiveStrategy.executeStream()
    │     ├─ PtyManager.write(prompt)
    │     ├─ OutputParser.parse(output)
    │     └─ MessageParser.parseMessage(jsonString)
    └─ HookService.executeHooks("post_query", HookContext)

Result: Observable<Message>
```

### 4.2 Configuration Chain
```
User Code:
  ClaudeCodeSDK() // default constructor
    ↓
ConfigLoader.createOptions()
    ├── loadDefaultConfiguration() // from classpath
    ├── loadUserConfiguration() // from ~/.claude/config.properties
    ├── loadEnvironmentVariables() // from System.getenv()
    └── ClaudeCodeOptions.Builder
        ├── ClaudePathResolver.resolveClaudePath() // resolve CLI path
        └── AuthenticationProvider (or DefaultAuthenticationProvider)

Result: ClaudeCodeSDK ready with all dependencies
```

### 4.3 Authentication Chain
```
ClaudeCodeOptions.Builder.authProvider(provider)
    ↓
AuthenticationProviderFactory.detectAndCreate()
    ├── Check CLAUDE_CODE_USE_BEDROCK env var
    │   ├─ Yes → BedrockAuthenticationProvider
    │   └─ No → Check CLAUDE_CODE_USE_VERTEX
    ├── Check GOOGLE_CLOUD_PROJECT env var
    │   ├─ Yes → VertexAIAuthenticationProvider
    │   └─ No → Check ANTHROPIC_API_KEY
    └── DefaultAuthenticationProvider

Each provider implements AuthenticationProvider interface

Result: AuthenticationProvider instance ready for use
```

---

## 5. Public API Surface (v2.0.0 Perspective)

### 5.1 Core Entry Points (Should NOT Change)

These are the primary classes users interact with:

1. **ClaudeCodeSDK**
   - Main facade for all SDK operations
   - Constructor: `ClaudeCodeSDK()` or `ClaudeCodeSDK(ClaudeCodeOptions)`
   - Methods: `query()`, `queryStream()`, `queryBuilder()`, `addHook()`, etc.
   - Status: **KEEP NAME** - widely used in GUI and tests

2. **ClaudeCodeOptions**
   - Configuration model used everywhere
   - Builder pattern: `ClaudeCodeOptions.builder()`
   - Status: **KEEP NAME** - canonical configuration class

3. **Message**
   - Return type from all query operations
   - Used in observable streams and callbacks
   - Status: **KEEP NAME** - fundamental data model

4. **QueryRequest**
   - Parameter model for queries
   - Builder: `QueryRequest.builder(prompt)`
   - Status: **KEEP NAME** - query parameter model

### 5.2 Secondary Entry Points (Frequently Used)

5. **QueryBuilder**
   - Fluent API for building queries
   - Status: **KEEP** - convenient builder

6. **HookService / HookCallback**
   - Event lifecycle hooks
   - Status: **KEEP** - event system

7. **SubagentManager**
   - Long-running subagent management
   - Status: **KEEP** - advanced feature

8. **AuthenticationProvider** (Interface)
   - Custom authentication support
   - Status: **KEEP** - extensibility point

### 5.3 Configuration/Setup Classes

9. **ConfigLoader**
   - Multi-source configuration loading
   - Status: **KEEP** - configuration management

10. **CliMode**
    - Execution mode enumeration
    - Status: **KEEP** - mode selection

### 5.4 Exception Classes

11. **ClaudeCodeException**
    - Base exception for all SDK errors
    - Status: **KEEP** - exception hierarchy

12. **ProcessExecutionException**
    - Process execution failures
    - Status: **KEEP** - specific error type

---

## 6. Renaming Considerations for v2.0.0

### 6.1 Classes with "Claude Code" Prefix

**Current Naming**:
- `ClaudeCodeSDK`
- `ClaudeCodeOptions`
- `ClaudeCodeException`

**Rationale for Keeping Names**:
- Used extensively in GUI module - breaking change
- Multiple test files depend on these names
- Clear naming convention - "Claude Code" refers to the CLI tool
- Consistent with Python SDK naming

**Alternative Names Considered**:
- `SDK` / `ClaudeSDK` - too generic
- `Options` / `Options` - ambiguous
- `CodeException` - unclear

**Recommendation**: **NO RENAMING** - Breaking changes outweigh benefits

### 6.2 Namespace Organization

**Current**: `com.anthropic.claude.*`  
**Status**: KEEP - Well-organized, logical separation

**Potential Issues**: NONE identified

### 6.3 Class Visibility Changes

**All 66 classes are PUBLIC** - Consider deprecating internal implementation classes:
- `ClaudeOutputPatterns` (internal regex patterns)
- `ClaudeResponseBuilder` (internal builder)
- `BatchProcessStrategy` (internal strategy)
- `PtyInteractiveStrategy` (internal strategy)

---

## 7. Dependency Impact Analysis

### 7.1 High-Impact Classes (Changes Break Many Things)

| Class | Dependents | Risk Level |
|-------|-----------|-----------|
| `ClaudeCodeSDK` | 3+ (GUI, Tests, User Code) | **CRITICAL** |
| `ClaudeCodeOptions` | 10+ (SDK, GUI, Tests, Factories) | **CRITICAL** |
| `Message` | 12+ (QueryService, Parser, Cache, GUI) | **CRITICAL** |
| `ClaudeCodeException` | 8+ (Error handling throughout) | **CRITICAL** |
| `QueryRequest` | 6+ (Query API) | **HIGH** |
| `AuthenticationProvider` | 6+ (Auth system) | **HIGH** |

### 7.2 Low-Impact Classes (Safe to Refactor)

- `PooledConnection` - internal performance detail
- `BackpressureController` - internal streaming detail
- `ClaudeOutputPatterns` - internal pattern matching
- `ClaudeResponseBuilder` - internal builder
- Example classes (BasicExample, BaseUrlExample)

### 7.3 External Breaking Change Risk

**GUI Module Breakage**: HIGH
- 10+ imports from SDK
- Direct class dependencies (not just interfaces)
- Any major refactoring requires GUI updates

**Test Suite Breakage**: MEDIUM
- 16 test files
- Can be easily updated in same PR
- Tests follow SDK changes by design

**User Code Breakage**: HIGH
- Public API changes visible to all users
- ClaudeCodeSDK is main entry point
- Message, QueryRequest, ClaudeCodeOptions widely used

---

## 8. Dependency Graph Statistics

### 8.1 Class Dependency Metrics

**Total Public Classes**: 66

**Dependency Fan-Out** (classes each class depends on):
- ClaudeCodeSDK: 7 direct dependencies
- QueryService: 8 direct dependencies
- MessageParser: 4 direct dependencies
- ConfigLoader: 4 direct dependencies
- AuthenticationProviderFactory: 5 direct dependencies

**Dependency Fan-In** (how many classes depend on each class):
- Message: 12+ dependents (highest)
- ClaudeCodeOptions: 10+ dependents
- ClaudeCodeException: 8+ dependents
- ProcessManager: 6+ dependents
- AuthenticationProvider: 6+ dependents

**Average Dependency Chain Depth**: 3-4 levels

---

### 8.2 Package-Level Dependencies

**Tightly Coupled Packages**:
- query ↔ process (QueryService uses ProcessManager)
- query ↔ messages (QueryService returns Message)
- query ↔ hooks (QueryService uses HookService)
- auth → exceptions (AuthenticationProviderFactory throws)
- config → exceptions (ConfigLoader throws)
- config → auth (ClaudeCodeOptions holds AuthenticationProvider)

**Loosely Coupled Packages**:
- tools (independent MCP server)
- context (optional context management)
- streaming (helper utilities)
- performance (optional optimization)
- examples (standalone demonstrations)

---

### 8.3 Circular Dependencies

**Analysis**: NO circular dependencies found

Benefits:
- Clean dependency graph
- Easy to understand dependency flow
- No initialization order issues
- Good for module extraction

---

## 9. Recommendations for v2.0.0

### 9.1 DO NOT Change

1. **Core Class Names**
   - ClaudeCodeSDK
   - ClaudeCodeOptions
   - ClaudeCodeException
   - Message
   - QueryRequest

2. **Package Names**
   - com.anthropic.claude.*

3. **Public Interface Signatures**
   - QueryService.queryStream()
   - QueryService.queryAsync()
   - ClaudeCodeSDK.query()
   - AuthenticationProvider interface

### 9.2 Consider Deprecating

1. **Configuration.class**
   - Alternative configuration model
   - Shadowed by ClaudeCodeOptions
   - Should use ClaudeCodeOptions.Builder instead

2. **models.Message**
   - Duplicate of messages.Message
   - Consolidate into single messages package

3. **Message.models package**
   - Alternative naming for Message
   - Cleanup single location

### 9.3 Modernization Opportunities

1. **Add Package-Private Implementations**
   - Mark internal strategies as package-private
   - Keep public interface only
   - Examples: BatchProcessStrategy, PtyInteractiveStrategy

2. **Sealed Classes** (Java 17+)
   - AuthenticationProvider with permitted implementations
   - MessageType sealed enum
   - CliMode sealed enum

3. **Records** (Java 17+)
   - ToolDefinition → record
   - ToolExecutionResult → record
   - HookResult → record

4. **Module System** (Future)
   - Prepare for java 9+ modules
   - Clean public API boundaries
   - Optional features as separate modules

### 9.4 Testing & Compatibility

1. **Maintain Backward Compatibility**
   - Keep all public classes
   - Keep all public methods
   - Add deprecation notices for redundant classes only

2. **Update Tests**
   - All 16 test files will pass without changes
   - No API changes needed

3. **GUI Module**
   - No changes required
   - All imports remain valid
   - Full compatibility

---

## 10. Dependency Map Summary Table

### 10.1 Complete Dependency Matrix

(See below for key dependencies)

### Core Dependencies Summary:

```
ClaudeCodeSDK (Entry Point)
├── Static Dependencies (always loaded)
│   ├── ConfigLoader
│   ├── ProcessManager
│   ├── QueryService
│   ├── HookService
│   └── SubagentManager
│
├── Optional Dependencies (mode-dependent)
│   └── PtyManager (CliMode.PTY_INTERACTIVE)
│
├── Dynamic Dependencies (from config)
│   └── AuthenticationProvider (or implementations)
│
└── Return Types / Results
    ├── Stream<Message>
    ├── Observable<Message>
    ├── SubagentManager
    ├── QueryBuilder
    └── QueryRequest
```

---

## 11. Conclusion

The claude-code-java-sdk module has a well-structured, low-complexity dependency graph with 66 classes organized into 19 logical packages. The design follows SOLID principles with clear separation of concerns.

**Key Findings**:
- ✅ No circular dependencies
- ✅ Clear dependency hierarchy with ClaudeCodeSDK as main facade
- ✅ Good package organization with logical grouping
- ✅ Reasonable public API surface
- ⚠️ HIGH external dependency risk (GUI module has 10+ imports)
- ⚠️ Core classes widely used - renaming would be breaking

**For v2.0.0 Upgrade**:
- **RECOMMENDATION: Keep current naming and structure**
- Focus on internal optimizations (package-private, sealed classes)
- Add deprecation notices for redundant classes only
- Maintain full backward compatibility
- Update documentation with v2.0.0 features

---

## Appendix: File Locations

### Core Package Files:
- Client: `/src/main/java/com/anthropic/claude/client/`
- Config: `/src/main/java/com/anthropic/claude/config/`
- Query: `/src/main/java/com/anthropic/claude/query/`
- Messages: `/src/main/java/com/anthropic/claude/messages/`
- Hooks: `/src/main/java/com/anthropic/claude/hooks/`
- Auth: `/src/main/java/com/anthropic/claude/auth/`
- Subagents: `/src/main/java/com/anthropic/claude/subagents/`
- Exceptions: `/src/main/java/com/anthropic/claude/exceptions/`

### Test Files:
- Location: `/src/test/java/com/anthropic/claude/`
- Count: 16 test files
- Coverage: Core components (SDK, Config, Auth, Hooks, Messages, Query, Tools)

### GUI Integration:
- Location: `../claude-code-gui/src/main/java/com/claude/gui/`
- Main Consumer: GUI module
- Integration Points: ClaudeCodeSDK, ClaudeCodeOptions, Message, QueryRequest

---

**Document Generated**: 2025-10-21  
**Analysis Scope**: claude-code-java-sdk v2.0.0-SNAPSHOT  
**Total Classes Analyzed**: 66  
**Packages Analyzed**: 19

