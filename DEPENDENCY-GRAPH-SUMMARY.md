# Dependency Graph Analysis Summary - Claude Code Java SDK

## Overview

A comprehensive dependency analysis has been completed for the `claude-code-java-sdk` module, documenting all 66 public classes across 19 packages.

**Analysis Output**: `/home/user/claude-code-java-sdk/dependency-graph.md` (1,072 lines)

## Key Metrics

| Metric | Value |
|--------|-------|
| Total Classes | 66 |
| Public Classes | 66 |
| Packages | 19 |
| Circular Dependencies | 0 (✅ NONE) |
| Core Entry Points | 4 (ClaudeCodeSDK, QueryRequest, Message, ClaudeCodeOptions) |
| External Consumers | 2 (GUI Module, Test Suite) |

---

## Package Inventory

### Tier 1: Core Public API (8 packages)

1. **com.anthropic.claude.client** (5 classes)
   - `ClaudeCodeSDK` - Main facade
   - `ClaudeSDKClient` - Session wrapper
   - `SessionManager` - Multi-session management
   - `MessageReceiver` - Message callback
   - `InterruptHandler` - Signal handling

2. **com.anthropic.claude.config** (4 classes)
   - `ClaudeCodeOptions` - Configuration model
   - `ConfigLoader` - Multi-source loader
   - `CliMode` - Execution mode enum
   - `Configuration` - Alternative config

3. **com.anthropic.claude.exceptions** (2 classes)
   - `ClaudeCodeException` - Base exception
   - `ProcessExecutionException` - Process errors

4. **com.anthropic.claude.hooks** (4 classes)
   - `HookService` - Hook execution engine
   - `HookCallback` - Callback interface
   - `HookContext` - Hook event context
   - `HookResult` - Hook execution result

5. **com.anthropic.claude.auth** (5 classes)
   - `AuthenticationProvider` - Interface
   - `DefaultAuthenticationProvider` - API key auth
   - `BedrockAuthenticationProvider` - AWS Bedrock
   - `VertexAIAuthenticationProvider` - Google Vertex AI
   - `AuthenticationProviderFactory` - Auth factory

6. **com.anthropic.claude.query** (3 classes)
   - `QueryRequest` - Query parameters
   - `QueryBuilder` - Fluent query builder
   - `QueryService` - Query execution engine

7. **com.anthropic.claude.messages** (3 classes)
   - `Message` - Message data model
   - `MessageParser` - JSON parsing
   - `MessageType` - Message type enum

8. **com.anthropic.claude.subagents** (2 classes)
   - `SubagentManager` - Subagent lifecycle
   - `Subagent` - Individual agent

### Tier 2: Supporting Infrastructure (11 packages)

9. **com.anthropic.claude.process** (2 classes)
   - Process execution management (ZT-Exec wrapper)

10. **com.anthropic.claude.strategy** (3 classes)
    - Execution strategy pattern (Batch/PTY)

11. **com.anthropic.claude.pty** (7 classes)
    - PTY management for interactive sessions

12. **com.anthropic.claude.tools** (6 classes)
    - MCP custom tools system

13. **com.anthropic.claude.context** (4 classes)
    - Context management & compression

14. **com.anthropic.claude.streaming** (5 classes)
    - RxJava streaming utilities

15. **com.anthropic.claude.performance** (3 classes)
    - Connection pooling & caching

16. **com.anthropic.claude.utils** (1 class)
    - Cross-platform utilities

17. **com.anthropic.claude.models** (1 class)
    - Alternative message model (legacy)

18. **com.anthropic.claude.examples** (2 classes)
    - Example implementations

---

## Dependency Hierarchy

### Central Facade: ClaudeCodeSDK

```
┌──────────────────────────────────────────┐
│          ClaudeCodeSDK (Facade)           │
│         Main entry point for users        │
└──────────┬───────────────────────────────┘
           │
      ┌────┼────┬──────────┬──────────┬─────────────┬──────────┐
      │    │    │          │          │             │          │
      ▼    ▼    ▼          ▼          ▼             ▼          ▼
   Config Process Query    Hooks   Subagents     Auth      InterruptHandler
   Loader Manager Service  Service  Manager    Providers
      │    │    │    │      │        │           │
      └────┼────┴────┼──────┼────────┴───────────┘
           │         │      │
           ▼         ▼      ▼
        ProcessMgr  Message  HookContext
        Strategy    Parser   HookResult
        Execution   
```

### High-Impact Classes (Changing breaks many things)

**Fan-In Analysis** (most depended upon):

| Class | Dependents | Risk |
|-------|-----------|------|
| Message | 12+ | CRITICAL |
| ClaudeCodeOptions | 10+ | CRITICAL |
| ClaudeCodeException | 8+ | CRITICAL |
| QueryRequest | 6+ | HIGH |
| ProcessManager | 6+ | HIGH |
| AuthenticationProvider | 6+ | HIGH |

---

## External Usage Analysis

### GUI Module (claude-code-gui)
**Import Count**: 10 SDK classes

```
com.anthropic.claude.client.ClaudeCodeSDK
com.anthropic.claude.config.ClaudeCodeOptions
com.anthropic.claude.config.CliMode
com.anthropic.claude.messages.Message
com.anthropic.claude.query.QueryRequest
com.anthropic.claude.pty.ClaudeResponse
com.anthropic.claude.pty.ClaudeState
com.anthropic.claude.pty.PtyManager
com.anthropic.claude.pty.StateChange
com.anthropic.claude.auth.DefaultAuthenticationProvider
```

**Breaking Change Risk**: HIGH
- Direct class dependencies (not interface-based)
- Rename ClaudeCodeSDK → breaks GUI
- Rename ClaudeCodeOptions → breaks GUI

### Test Suite
**Test Files**: 16

- ClaudeCodeSDKTest
- ConfigLoaderTest
- AuthenticationProviderFactoryTest
- HookServiceTest
- MessageParserTest
- MessageTest
- ProcessManagerTest
- QueryRequestTest
- And 8 others...

**Breaking Change Risk**: MEDIUM
- Can be updated in same PR
- Tests follow SDK changes by design

---

## Dependency Chains (Transitive Examples)

### Query Execution Chain
```
User: sdk.queryStream("prompt")
  ↓
ClaudeCodeSDK.queryStream(QueryRequest)
  ↓
QueryService.queryStream(QueryRequest)
  ├── HookService.executeHooks("pre_query")
  ├── CliExecutionStrategy.executeStream()
  │  ├─ ProcessManager.executeStreaming()
  │  └─ MessageParser.parseMessage()
  └── HookService.executeHooks("post_query")
  
Result: Observable<Message>
```

### Configuration Chain
```
ClaudeCodeSDK()
  ↓
ConfigLoader.loadConfiguration()
  ├── Load: classpath/claude-code.properties
  ├── Load: ~/.claude/config.properties
  ├── Load: System.getenv()
  └── ClaudeCodeOptions.Builder
      ├── ClaudePathResolver.resolveClaudePath()
      └── AuthenticationProvider
  
Result: ClaudeCodeSDK ready to use
```

### Authentication Chain
```
AuthenticationProviderFactory.detectAndCreate()
  ├── Check: CLAUDE_CODE_USE_BEDROCK
  │   → BedrockAuthenticationProvider
  ├── Check: CLAUDE_CODE_USE_VERTEX
  │   → VertexAIAuthenticationProvider
  └── Default: ANTHROPIC_API_KEY
      → DefaultAuthenticationProvider

All implement: AuthenticationProvider
```

---

## v2.0.0 Upgrade Recommendations

### ✅ DO NOT CHANGE

1. **Core Class Names**
   - `ClaudeCodeSDK` - Used in GUI and tests
   - `ClaudeCodeOptions` - Configuration model everywhere
   - `ClaudeCodeException` - Exception hierarchy
   - `Message` - Fundamental data model
   - `QueryRequest` - Query parameter model

2. **Package Names**
   - `com.anthropic.claude.*` - Well-organized

3. **Public Method Signatures**
   - All query methods stable
   - All factory methods stable
   - All builder methods stable

### ⚠️ CONSIDER DEPRECATING

1. `Configuration` class
   - Alternative config model
   - Use `ClaudeCodeOptions` instead

2. `com.anthropic.claude.models.Message`
   - Duplicate of `messages.Message`
   - Consolidate to single location

### 🚀 MODERNIZATION OPPORTUNITIES

1. **Package-Private Implementations**
   - `BatchProcessStrategy`
   - `PtyInteractiveStrategy`
   - Internal message builders
   - Keep public interfaces only

2. **Sealed Classes** (Java 17+)
   - Seal `AuthenticationProvider`
   - Seal `MessageType` enum
   - Seal `CliMode` enum

3. **Records** (Java 17+)
   - `ToolDefinition` → record
   - `ToolExecutionResult` → record
   - `HookResult` → record

4. **Module System** (Future)
   - Prepare for Java 9+ modules
   - Clean public API boundaries

---

## Quality Assessment

### Strengths ✅

- **Clean Dependency Graph**: No circular dependencies
- **Clear Separation of Concerns**: Well-organized packages
- **Single Responsibility**: Each class has clear purpose
- **Facade Pattern**: ClaudeCodeSDK provides unified interface
- **Factory Pattern**: Consistent object creation
- **Builder Pattern**: Fluent configuration APIs
- **Strategy Pattern**: Pluggable execution strategies
- **Observer Pattern**: Hook system for events

### Areas to Watch ⚠️

- **High External Coupling**: GUI has 10+ direct imports (breaking change risk)
- **Large Core Classes**: ClaudeCodeSDK and QueryService have multiple responsibilities
- **Alternative Models**: Configuration and Message have duplicates

---

## Circular Dependency Check

**Result**: ✅ **NONE FOUND**

Verification Method:
- Static analysis of import statements
- Manual trace of dependency chains
- Package-level dependency graph construction

Benefits:
- Easy module extraction
- No initialization order issues
- Clear dependency flow
- Good for refactoring

---

## Statistics

### Class Distribution
- Public Classes: 66 (100%)
- Package-Private Classes: 0
- Private Classes: 0

### Dependency Complexity
- Average Class Dependencies: 3-4
- Maximum Class Dependencies: ClaudeCodeSDK (7)
- Average Dependent Count: 2-3
- Maximum Dependent Count: Message (12+)

### Package Complexity
- Tightly Coupled: client↔config↔query↔hooks
- Loosely Coupled: tools, context, streaming, performance
- Independent Packages: examples, utils

---

## File Locations (Reference)

### Analysis Document
- **Full Analysis**: `/home/user/claude-code-java-sdk/dependency-graph.md` (1,072 lines)
- **Summary**: This document

### Source Locations
- **SDK Source**: `/home/user/claude-code-java-sdk/claude-code-java-sdk/src/main/java/com/anthropic/claude/`
- **Tests**: `/home/user/claude-code-java-sdk/claude-code-java-sdk/src/test/java/com/anthropic/claude/`
- **GUI Module**: `/home/user/claude-code-java-sdk/claude-code-gui/src/main/java/com/claude/gui/`

---

## Conclusion

The claude-code-java-sdk module demonstrates excellent software architecture with a clean, comprehensible dependency graph. The design successfully balances feature richness with maintainability.

**For v2.0.0 release**:
- Keep all core class names and signatures
- Maintain full backward compatibility
- Focus on internal optimizations
- Add deprecation notices only for redundant classes
- Update documentation with new features

**Risk Level for Renaming**: **VERY HIGH**
- GUI module has 10+ direct class imports
- Test suite expects current names
- User code depends on public API

**Recommendation**: **Maintain Current Structure** ✅

---

Generated: 2025-10-21  
Total Classes Analyzed: 66  
Packages Analyzed: 19  
Analysis Depth: Very Thorough
