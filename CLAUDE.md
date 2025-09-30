# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java-based multi-module Maven project that provides a comprehensive Java SDK and GUI for interacting with Claude Code CLI. The project consists of two main modules:

1. **claude-code-java-sdk**: A comprehensive Java SDK that provides 1:1 functionality parity with the official **Claude Code Python SDK** (`claude-code-sdk`)
2. **claude-code-gui**: A Java Swing-based GUI application for interactive Claude Code CLI sessions

**IMPORTANT**: This project translates the **Claude Code Python SDK**, not the Anthropic Messages API SDK. The Claude Code SDK is a high-level programming interface built on top of Claude Code CLI, providing features like hooks, subagents, custom tools, and context management.

**Build Tool**: Maven 3.8+
**Java Version**: Java 17+
**Development Environment**: Windows 11
**Coding Standards**: Alibaba P3C

## Build and Development Commands

### Core Maven Commands
```bash
# Build entire project (all modules)
mvn clean compile

# Run tests for all modules
mvn test

# Package all modules
mvn clean package

# Install to local Maven repository
mvn install

# Build without tests
mvn clean package -DskipTests
```

### Module-Specific Commands
```bash
# Work with SDK module only
cd claude-code-java-sdk
mvn compile
mvn test
mvn package

# Work with GUI module only
cd claude-code-gui
mvn compile
mvn exec:java  # Runs the GUI application
```

### Running the GUI Application
```bash
# From root directory
java -jar claude-code-gui/target/claude-code-gui-1.0.0.jar

# Or using Maven exec plugin
cd claude-code-gui
mvn compile exec:java
```

### Testing Commands
```bash
# Run specific test class
mvn test -Dtest=ClaudeCodeSDKTest

# Run specific test method
mvn test -Dtest=ClaudeCodeSDKTest#testBasicQuery

# Run tests in specific module
mvn test -pl claude-code-java-sdk
mvn test -pl claude-code-gui
```

## Project Architecture

### Multi-Module Structure

The project follows a parent-child Maven module structure:

```
claude-code-parent/
├── claude-code-java-sdk/     # Java SDK module (75 classes)
├── claude-code-gui/          # Swing GUI module (16 classes)
└── pom.xml                   # Parent POM with dependency management
```

### Claude Code Java SDK Architecture

The SDK provides comprehensive Claude Code CLI integration with enterprise features:

**Core Components:**
- **ClaudeCodeSDK**: Main facade and entry point for all SDK functionality
- **QueryService**: Handles query execution with RxJava reactive streaming
- **ProcessManager**: Cross-platform process execution using ZT-Exec
- **MessageParser**: JSON response parsing using Jackson
- **ConfigLoader**: Multi-source configuration management

**Advanced Features:**
- **Custom Tools System**: In-process MCP server with @Tool annotations
- **Authentication Providers**: Multi-cloud support (Direct API, AWS Bedrock, Google Vertex AI)
- **Hook System**: Event-driven lifecycle hooks
- **Context Management**: Automatic context compression and window management
- **Subagent Management**: Long-running Claude subprocess management

**Package Structure:**
```
com.anthropic.claude/
├── client/          # Main SDK client and session management
├── config/          # Configuration loading and validation
├── auth/            # Authentication providers
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

### Claude Code GUI Architecture

Modern Swing application with session management and streaming Claude CLI integration:

**Core Components:**
- **ClaudeCodeGUI**: Main application entry point
- **MainWindow**: Primary GUI interface with split-pane layout
- **SessionManager**: Multi-session management with --resume support
- **ClaudeCliExecutor**: Process management for Claude CLI
- **StreamReader**: Real-time output streaming from CLI processes

**Key Features:**
- Multi-session support with session switching
- Real-time streaming responses
- Session persistence and management
- Modern split-pane UI design
- Windows environment optimized

**Package Structure:**
```
com.claude.gui/
├── callback/        # Message callback interfaces
├── executor/        # CLI process execution
├── model/           # Data models (ChatSession, Message)
├── service/         # Session management services
├── stream/          # Stream readers for real-time output
└── ClaudeCodeGUI    # Main application class
```

## Key Dependencies

### Core Dependencies (Managed in Parent POM)
- **Jackson**: JSON processing (v2.16.1)
- **OkHttp**: HTTP client (v4.12.0)
- **RxJava**: Reactive programming (v3.1.8)
- **ZT-Exec**: Process execution (v1.12)
- **Caffeine**: High-performance caching (v3.1.8)
- **SLF4J + Logback**: Logging framework

### GUI-Specific Dependencies
- **FlatLaf**: Modern Swing Look & Feel (v3.2.5)
- **MigLayout**: Advanced layout manager (v11.0)
- **RSyntaxTextArea**: Syntax highlighting text area (v3.3.4)

### Test Dependencies
- **JUnit 5**: Testing framework (v5.10.1)
- **Mockito**: Mocking framework (v4.11.0)

## Configuration

### Environment Variables
```bash
# Authentication
ANTHROPIC_API_KEY=your-api-key

# CLI Configuration
CLAUDE_CODE_CLI_PATH=claude-code
CLAUDE_CODE_TIMEOUT_SECONDS=600
CLAUDE_CODE_MAX_RETRIES=3

# Cloud Provider Support
CLAUDE_CODE_USE_BEDROCK=false
CLAUDE_CODE_USE_VERTEX=false
```

### Configuration File
Location: `~/.claude/config.properties`
```properties
api.key=your-api-key
cli.path=claude-code
timeout.seconds=600
max.retries=3
logging.enabled=true
```

## Development Guidelines

### Platform Considerations
- **Target Environment**: Windows 11 (primary development environment)
- **Cross-Platform**: Code must work on Windows/macOS/Linux
- **Path Handling**: Use `ClaudePathResolver` for CLI path resolution
- **Process Management**: Handle Windows-specific process behavior

### Code Standards
- **Follow Alibaba P3C coding standards** strictly
- Use meaningful English variable/method names
- All public APIs require JavaDoc comments
- Prefer composition over inheritance
- Implement comprehensive error handling and logging
- **No code modifications beyond requirements**

### Design Patterns Used
- **Builder Pattern**: Configuration and request building
- **Factory Pattern**: Authentication provider selection
- **Strategy Pattern**: Different authentication strategies
- **Observer Pattern**: Hook system for events
- **Command Pattern**: Query execution with retry/timeout
- **Singleton**: SDK instance and service managers

## Common Development Tasks

### Adding New Authentication Provider
1. Implement `AuthenticationProvider` interface
2. Add factory method in `AuthenticationProviderFactory`
3. Add environment variable support in configuration
4. Update documentation

### Adding Custom MCP Tools
1. Create method with `@Tool` annotation
2. Define proper parameter types
3. Return appropriate result object
4. Register with MCPServer

### Adding GUI Features
1. Follow existing session management patterns
2. Use SwingUtilities.invokeLater for thread safety
3. Integrate with existing SessionManager
4. Follow MigLayout patterns for UI layout

### Debugging Common Issues
1. **SDK Issues**: Enable debug logging, check CLI path resolution
2. **GUI Issues**: Verify Claude CLI connection, check session state
3. **Process Issues**: Monitor ProcessManager execution logs
4. **Configuration Issues**: Validate with ConfigLoader.validateConfiguration()

## Testing Strategy

### Unit Testing
- All core components must have unit tests
- Use Mockito for external dependencies
- Target >80% code coverage
- Test both success and failure scenarios

### Integration Testing
- Test with actual Claude CLI when possible
- Mock external dependencies in CI environments
- Test cross-platform compatibility

### GUI Testing
- Test session management functionality
- Verify UI thread safety
- Test long-running session scenarios

## Project Status

**Current State**: Production-ready multi-module project
- **SDK Module**: v2.0.0-SNAPSHOT (95% complete, enterprise-ready)
- **GUI Module**: v2.0.0 Enhanced (full session management features)

**Key Achievements:**
- 100% Claude Code Python SDK functionality parity
- Advanced enterprise features (hooks, subagents, custom tools)
- Modern multi-session GUI with --resume support
- Windows environment optimization
- Comprehensive error handling and logging

## Important Notes

### When Working with This Codebase
- **READ FIRST**: Always check existing module-specific CLAUDE.md files for detailed information
- **BUILD ORDER**: Always build from root directory to ensure proper dependency resolution
- **TESTING**: Run tests after any significant changes
- **WINDOWS FOCUS**: Consider Windows-specific behavior and paths
- **NO UNNECESSARY CHANGES**: Only modify code as specifically requested