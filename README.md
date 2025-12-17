
## Features

### 🌍 Dynamic World Simulation
- **Procedural Terrain Generation**: Realistic elevation, biomes, and climate zones
- **Seasonal Climate System**: Temperature variations, rainfall patterns, and seasonal effects
- **Resource Management**: Food, wood, metals, fossils with renewal and depletion mechanics
- **Random Events**: Volcanic eruptions, earthquakes affecting global climate

### 👥 Agent-Based Population
- **Humans**: Individual agents with genetics, culture, needs, and behaviors
- **Animals**: Livestock (cows, chickens, sheep, horses) and wild game
- **Behaviors**: Gathering, hunting, farming, reproduction, migration
- **Genetics**: Unique identifiers reflecting genetic proximity
- **Culture**: Cultural attachment and diffusion mechanics

### 📊 Statistics & Analysis
- **Population Tracking**: Real-time population graphs
- **Economic Indicators**: Gini coefficient, resource distribution
- **Biomass Calculation**: Track total biomass across species
- **Energy Metrics**: Energy per capita, technological efficiency
- **Information Diffusion**: Track communication speed (roads, horses, telegraph, internet)

### 🖥️ Modern User Interface
- **Responsive Design**: Adaptive layout for different screen sizes
- **Interactive Map**: Pan, zoom, and hover for detailed information
- **Real-time Controls**: Start, pause, speed adjustment (1x, 5x, 20x)
- **Statistics Dashboard**: Charts and graphs for key metrics
- **Internationalization**: English, French, Spanish, German

### 🎯 Advanced Features
- **Multithreading**: Efficient simulation using Java Virtual Threads
- **Configuration**: JSON-based world generation and simulation parameters
- **Save/Load**: Persistent simulation states
- **Comprehensive Logging**: Timestamped logs for debugging and analysis
- **Extensive Testing**: 70%+ code coverage with unit and integration tests

## Requirements

- **Java**: JDK 21 or higher ([Download](https://adoptium.net/))
- **Maven**: 3.9+ ([Download](https://maven.apache.org/download.cgi))

## Quick Start

### Installation

```bash
# Clone the repository
git clone https://github.com/yourusername/ether-society-simulation.git
cd ether-society-simulation

# Build the project
mvn clean install

# Run the application
mvn javafx:run
```

### Running Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn clean test jacoco:report
# Coverage report at target/site/jacoco/index.html
```

### Building Distribution

```bash
# Create executable JAR
mvn clean package

# Run standalone JAR
java -jar target/society-simulation-2.0.0.jar
```

## Usage

### Basic Controls
- **Start**: Begin the simulation from 20,000 BC
- **Pause**: Pause the simulation
- **Speed**: Adjust simulation speed (1x, 5x, 20x)
- **Reset**: Restart from the beginning

### Map Interaction
- **Hover**: Show detailed cell information (biome, temperature, resources, population)
- **Pan**: Drag to navigate the map
- **Zoom**: Use mouse wheel to zoom in/out

### Menu Options
- **File**: New simulation, Load, Save, Exit
- **View**: Toggle layers (temperature, resources, population density)
- **Simulation**: Settings, pause, reset
- **Help**: User guide, about, credits

## Configuration

Edit `src/main/resources/config/default-config.json` to customize:
- World size and generation parameters
- Starting population and resources
- Climate settings
- Event probabilities

Example:
```json
{
  "world": {
    "width": 100,
    "height": 100,
    "seed": 12345
  },
  "simulation": {
    "startYear": -20000,
    "tickRate": 1000
  }
}
```

## Architecture

```
src/
├── main/
│   ├── java/com/ether/society/
│   │   ├── core/          # Simulation engine
│   │   ├── model/         # Domain models
│   │   ├── simulation/    # Simulation features
│   │   ├── agent/         # Agent behaviors
│   │   ├── ui/            # JavaFX interface
│   │   ├── i18n/          # Internationalization
│   │   ├── config/        # Configuration
│   │   └── util/          # Utilities
│   └── resources/
│       ├── config/        # JSON configs
│       ├── i18n/          # Translation files
│       └── css/           # Stylesheets
└── test/                  # Test suites
```

## Documentation

- [User Guide](docs/USER_GUIDE.md) - How to use the application
- [Developer Guide](docs/DEVELOPER_GUIDE.md) - Development setup and contribution
- [Architecture](docs/ARCHITECTURE.md) - Design decisions and patterns
- [API Documentation](docs/API.md) - Code API reference
- [Specifications](SPECIFICATIONS.md) - Technical specifications

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Testing

The project maintains high code quality with extensive testing:
- **Unit Tests**: Core logic and components
- **Integration Tests**: Full simulation cycles
- **Performance Tests**: Large-scale scenarios

Run tests with: `mvn test`

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Authors

- **Silvere Martin-Michiellot** - *Initial work and design* - [silvere.martin@gmail.com](mailto:silvere.martin@gmail.com)
- **AI Assistant (Antigravity/Claude)** - *Development assistance*

## Acknowledgments

- SugarScape model by Joshua M. Epstein and Robert Axtell
- JavaFX community for UI framework
- Open source contributors

## Roadmap

### Version 2.0 (Current)
- ✅ Modern Java 21 features
- ✅ Comprehensive UI with internationalization
- ✅ Multithreading with Virtual Threads
- ✅ Extensive test coverage

### Future Versions
- 🔲 3D visualization option
- 🔲 Multiplayer simulation
- 🔲 Advanced AI for agent decision-making
- 🔲 Historical event scenarios
- 🔲 Climate change modeling

## Support

For questions and support:
- Open an [issue](https://github.com/yourusername/ether-society-simulation/issues)
- Email: [silvere.martin@gmail.com](mailto:silvere.martin@gmail.com)

## Project Status

🚧 **Active Development** - Version 2.0 in progress
