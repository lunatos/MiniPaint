# MiniPaint

A Java Swing desktop application for creating and manipulating 2D shapes. Built for a Computer Graphics course.

## Features

- **Draw tools**: points, lines, circles, and polygons
- **Selection**: drag to select multiple shapes at once
- **Edit mode**: modify selected shapes
- **Pan & Rotate**: navigate and transform the canvas
- **Custom font** support via bundled FontAwesome icon

## Project Structure

| File | Description |
|---|---|
| `PaintApp.java` | Main entry point, sets up the window and toolbar |
| `CanvasPanel.java` | Drawing canvas with mouse interaction logic |
| `Definitions.java` | Shape hierarchy (`Shape`, `Point2D`, `Line2D`, `Circle2D`, `Polygon2D`) and `ToolMode` enum |
| `Utils.java` | Shared drawing utilities (colors, stroke) |

## Requirements

- Java JDK 8 or newer

## How to Run

```bash
# Compile all Java files
javac *.java

# Run the application
java PaintApp
```
