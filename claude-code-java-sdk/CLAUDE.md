# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java SDK for Claude Code - a 1:1 translation from the official Python SDK that provides comprehensive functionality for interacting with Claude through the CLI. The project implements advanced features including custom tools, multi-cloud authentication, and context management.

**Project**: Claude Code Java SDK
**Language**: Java 17+
**Build Tool**: Maven
**Package Structure**: `com.anthropic.claude.*`

## Build and Development Commands

### Build and Test
```bash
# Compile the project
mvn compile

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClaudeCodeSDKTest

# Run specific test method
mvn test -Dtest=ClaudeCodeSDKTest#testBasicQuery

# Clean and build
mvn clean compile

# Package the JAR
mvn package

# Install to local repository
mvn install
```

### Code Quality
```bash
# The project follows Alibaba P3C coding standards
# Use IDE plugins for real-time code style checking
```

## Architecture Overview

### Core Components

1. **ClaudeCodeSDK** (`client/ClaudeCodeSDK.java`)
   - Main entry point and facade for all SDK functionality
   - Manages lifecycle of all services and components
   - Supports both default and custom configuration

2. **QueryService** (`query/QueryService.java`)
   - Handles query execution through Claude CLI process
   - Supports synchronous and asynchronous operations
   - Implements retry logic and timeout management
   - Uses RxJava for reactive streaming

3. **ProcessManager** (`process/ProcessManager.java`)
   - Cross-platform process execution using ZT-Exec
   - Manages Claude CLI process lifecycle
   - Handles stdin/stdout/stderr streams
   - Implements process timeout and cleanup

4. **MessageParser** (`messages/MessageParser.java`)
   - Parses Claude CLI JSON responses using Jackson
   - Handles streaming JSON parsing for real-time responses
   - Defines message types and validation

5. **ConfigLoader** (`config/ConfigLoader.java`)
   - Loads configuration from multiple sources (env vars, files, code)
   - Supports `.claude/config.properties` file
   - Implements configuration validation and priority

### Advanced Features

1. **Custom Tools System** (`tools/MCPServer.java`)
   - In-process MCP server for custom tools
   - Uses `@Tool` annotation for method marking
   - Reflection-based tool execution

2. **Authentication Providers** (`auth/`)
   - Multi-cloud support: Direct API, AWS Bedrock, Google Vertex AI
   - Factory pattern for provider selection
   - Environment variable-based configuration

3. **Hook System** (`hooks/HookService.java`)
   - Event-driven hooks for query lifecycle
   - Pre/post query and error handling hooks
   - Functional interface-based callbacks

4. **Context Management** (`context/ContextManager.java`)
   - Automatic context compression and management
   - Smart context window handling
   - Message importance evaluation

5. **Subagent Management** (`subagents/SubagentManager.java`)
   - Manages long-running Claude subprocesses
   - Task distribution and monitoring
   - Inter-agent communication

## Package Structure

```
com.anthropic.claude/
├── client/          # Main SDK client and session management
├── config/          # Configuration loading and validation
├── auth/            # Authentication providers (Direct/Bedrock/Vertex)
├── query/           # Query execution and request building
├── messages/        # Message parsing and data models
├── process/         # CLI process management
├── hooks/           # Event hook system
├── subagents/       # Subagent lifecycle management
├── tools/           # Custom MCP tools system
├── context/         # Context management and compression
├── utils/           # Utility classes and helpers
└── exceptions/      # Custom exception hierarchy
```

## Configuration

### Environment Variables
- `ANTHROPIC_API_KEY` - Direct API authentication
- `CLAUDE_CODE_CLI_PATH` - Custom CLI path
- `CLAUDE_CODE_TIMEOUT_SECONDS` - Query timeout (default: 600)
- `CLAUDE_CODE_MAX_RETRIES` - Retry attempts (default: 3)
- `CLAUDE_CODE_USE_BEDROCK` - Enable AWS Bedrock
- `CLAUDE_CODE_USE_VERTEX` - Enable Google Vertex AI

### Configuration File
Location: `~/.claude/config.properties`
```properties
api.key=your-api-key
cli.path=claude-code
timeout.seconds=600
max.retries=3
logging.enabled=true
```

## Key Design Patterns

1. **Builder Pattern**: Used extensively for configuration and requests
2. **Factory Pattern**: Authentication provider selection
3. **Strategy Pattern**: Different authentication and context strategies
4. **Observer Pattern**: Hook system for events
5. **Command Pattern**: Query execution with retry and timeout
6. **Singleton**: SDK instance and service managers

## Dependencies

### Core Dependencies
- **Jackson**: JSON processing (`jackson-databind`, `jackson-annotations`)
- **OkHttp**: HTTP client for API calls
- **RxJava**: Reactive streaming support
- **ZT-Exec**: Cross-platform process execution
- **Caffeine**: High-performance caching
- **SLF4J + Logback**: Logging framework

### Test Dependencies
- **JUnit 5**: Unit testing framework
- **Mockito**: Mocking framework for tests

## Development Guidelines

### Code Standards
- Follow Alibaba P3C coding standards
- Use meaningful variable and method names in English
- All public APIs must have JavaDoc comments
- Prefer composition over inheritance
- Implement proper error handling and logging

### Platform Considerations
- Current development environment is Windows 11
- Code must be cross-platform compatible (Windows/macOS/Linux)
- Use `ClaudePathResolver` for CLI path resolution
- Handle path separators correctly

### Testing Strategy
- Unit tests for all core components
- Integration tests with actual Claude CLI
- Mock external dependencies in tests
- Test coverage should be > 80%

## Common Tasks

### Adding a New Tool
1. Create method with `@Tool` annotation
2. Define parameters with proper types
3. Return appropriate result object
4. Register with MCPServer

### Adding Authentication Provider
1. Implement `AuthenticationProvider` interface
2. Add factory method in `AuthenticationProviderFactory`
3. Add environment variable support
4. Update configuration documentation

### Debugging Query Issues
1. Enable debug logging: `logger.setLevel(DEBUG)`
2. Check CLI path resolution: `ClaudePathResolver.resolvePath()`
3. Validate configuration: `ConfigLoader.validateConfiguration()`
4. Monitor process execution in `ProcessManager`

## Current Status

**Version**: v2.0.0-SNAPSHOT (95% complete)
**Status**: Production-ready with advanced features
**Target**: v2.0.0 formal release

The project has successfully implemented 100% of Python SDK functionality plus additional enterprise features. Remaining tasks focus on test improvements and documentation enhancement.