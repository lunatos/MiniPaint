# MiniPaint

A Java Swing desktop application for creating and manipulating 2D shapes. Built for a Computer Graphics course.

## Features

- **Draw tools**: points, lines, circles, polygons, **Bézier curves**, and **Hermite curves**
- **Selection**: drag to select multiple shapes at once
- **Edit mode**: modify selected shapes
- **Pan & Rotate**: navigate and transform the canvas
- **Custom font** support via bundled FontAwesome icon

## Project Structure

| File | Description |
|---|---|
| `PaintApp.java` | Main entry point, sets up the window and toolbar |
| `CanvasPanel.java` | Drawing canvas with mouse interaction logic |
| `Definitions.java` | Shape hierarchy (`Shape`, `Point2D`, `Line2D`, `Circle2D`, `Polygon2D`, `BezierCurve2D`, `HermiteCurve2D`) and `ToolMode` enum |
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

---

## Curvas Paramétricas

### 1. Curva de Bézier Cúbica

#### Modelo Matemático

A curva de Bézier cúbica é definida por **4 pontos de controle** P₀, P₁, P₂, P₃ e avaliada pelo parâmetro t ∈ [0, 1]:

```
P(t) = (1-t)³·P₀ + 3·t·(1-t)²·P₁ + 3·t²·(1-t)·P₂ + t³·P₃
```

Os coeficientes são os **Polinômios de Bernstein** de grau 3:

| Polinômio | Fórmula        | Valor em t=0 | Valor em t=1 |
|-----------|----------------|:------------:|:------------:|
| B₀(t)     | (1-t)³         | 1            | 0            |
| B₁(t)     | 3·t·(1-t)²     | 0            | 0            |
| B₂(t)     | 3·t²·(1-t)     | 0            | 0            |
| B₃(t)     | t³             | 0            | 1            |

**Forma matricial equivalente:**

```
P(t) = [t³  t²  t  1] · M · [P₀  P₁  P₂  P₃]ᵀ

       | -1   3  -3   1 |
M  =   |  3  -6   3   0 |
       | -3   3   0   0 |
       |  1   0   0   0 |
```

#### Propriedades

- **Interpolação**: a curva passa por P₀ (t=0) e P₃ (t=1).
- **Envoltória convexa**: a curva está sempre contida na envoltória convexa dos 4 pontos.
- **Tangentes**: P'(0) = 3·(P₁ - P₀), P'(1) = 3·(P₃ - P₂).
- **Invariância afim**: translação, rotação e escala dos pontos de controle preservam a curva.

#### Como Usar

1. Selecione a ferramenta **"Béz"** na barra de ferramentas.
2. Clique **4 vezes** no canvas para definir P₀, P₁, P₂, P₃.
3. A curva é desenhada automaticamente após o 4º clique.
4. Durante a construção, um preview mostra a curva parcial.

#### Refinamento de Renderização

A curva é discretizada em **100 segmentos de reta** uniformemente espaçados em t. Cada segmento reutiliza a classe `Line2D` existente, garantindo que os algoritmos de **clipping** (Cohen-Sutherland / Liang-Barsky) e **rasterização** (DDA / Bresenham) se apliquem automaticamente.

---

### 2. Curva de Hermite Cúbica

#### Modelo Matemático

A curva de Hermite cúbica é definida por **2 pontos extremos** P₁, P₄ e **2 vetores tangente** R₁, R₄:

```
P(t) = h₁(t)·P₁ + h₂(t)·P₄ + h₃(t)·R₁ + h₄(t)·R₄,   t ∈ [0, 1]
```

Os **4 polinômios base de Hermite** são:

| Polinômio | Fórmula           | Significado                        |
|-----------|-------------------|------------------------------------|
| h₁(t)     | 2t³ - 3t² + 1     | h₁(0)=1, h₁(1)=0 (interpola P₁)   |
| h₂(t)     | -2t³ + 3t²        | h₂(0)=0, h₂(1)=1 (interpola P₄)   |
| h₃(t)     | t³ - 2t² + t      | h₃'(0)=1 (tangente em P₁ = R₁)    |
| h₄(t)     | t³ - t²           | h₄'(1)=1 (tangente em P₄ = R₄)    |

**Forma matricial equivalente:**

```
P(t) = [t³  t²  t  1] · M · [P₁  P₄  R₁  R₄]ᵀ

       |  2  -2   1   1 |
M  =   | -3   3  -2  -1 |
       |  0   0   1   0 |
       |  1   0   0   0 |
```

#### Relação com Bézier

Toda curva de Hermite pode ser convertida em Bézier:

```
P₀_bez = P₁
P₁_bez = P₁ + R₁/3
P₂_bez = P₄ - R₄/3
P₃_bez = P₄
```

#### Propriedades

- **Interpolação completa**: a curva passa por ambos P₁ e P₄.
- **Controle de tangentes**: R₁ e R₄ controlam a direção e magnitude da curvatura em cada extremo.
- **Invariância afim**: os pontos são transformados normalmente; as tangentes (vetores) são rotacionadas/escaladas sem translação.

#### Como Usar

1. Selecione a ferramenta **"Herm"** na barra de ferramentas.
2. **Clique** para definir P₁ (ponto inicial), depois **arraste** para definir R₁ (tangente em P₁).
3. **Clique** para definir P₄ (ponto final), depois **arraste** para definir R₄ (tangente em P₄).
4. A curva é desenhada ao soltar o segundo arraste.

#### Refinamento de Renderização

Idêntico à Bézier: 100 segmentos de reta uniformes em t, com clipping e rasterização automáticos via `Line2D`.

#### Visualização

- **Pontos extremos**: discos verdes.
- **Tangentes**: setas tracejadas (R₁ em azul, R₄ em laranja).
- **Bézier**: mostra o polígono de controle (linhas tracejadas) e pontos de controle (verdes = extremos, laranjas = intermediários).
