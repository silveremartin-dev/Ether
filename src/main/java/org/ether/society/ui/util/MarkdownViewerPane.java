/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.ether.society.i18n.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * High-performance, theme-adaptive Markdown viewer pane for JavaFX.
 * Renders headers, tables, bullet lists, bold text, code pills, and LaTeX expressions
 * into a rich native layout with an option to toggle raw markdown view.
 */
public class MarkdownViewerPane extends BorderPane {

    private final ScrollPane scrollPane;
    private final VBox formattedContainer;
    private final TextArea rawTextArea;
    private final ToggleButton toggleRawBtn;
    private final Button copyBtn;
    private String currentMarkdown = "";

    public MarkdownViewerPane() {
        getStyleClass().add("card-section");

        // Top Toolbar
        HBox toolBar = new HBox(8);
        toolBar.setAlignment(Pos.CENTER_RIGHT);
        toolBar.setPadding(new Insets(4, 8, 4, 8));
        toolBar.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 4;");

        Label formatHint = new Label(I18n.getOrDefault("markdown.preview_title", "✨ Formatted Report Preview"));
        formatHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        HBox.setHgrow(formatHint, Priority.ALWAYS);

        toggleRawBtn = new ToggleButton(I18n.getOrDefault("markdown.btn.toggle_raw", "📝 Code Brut"));
        toggleRawBtn.setStyle("-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 3 8;");
        toggleRawBtn.setOnAction(e -> updateViewMode());

        copyBtn = new Button(I18n.getOrDefault("markdown.btn.copy", "📋 Copier"));
        copyBtn.setStyle("-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 3 8;");
        copyBtn.setOnAction(e -> copyToClipboard());

        toolBar.getChildren().addAll(formatHint, copyBtn, toggleRawBtn);
        setTop(toolBar);

        // Formatted Content ScrollPane
        formattedContainer = new VBox(10);
        formattedContainer.setPadding(new Insets(12, 16, 16, 16));
        formattedContainer.setStyle("-fx-background-color: transparent;");

        scrollPane = new ScrollPane(formattedContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Raw Text Area
        rawTextArea = new TextArea();
        rawTextArea.setEditable(false);
        rawTextArea.setWrapText(true);
        rawTextArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;");

        setCenter(scrollPane);
    }

    public void setMarkdown(String markdown) {
        this.currentMarkdown = markdown != null ? markdown : "";
        rawTextArea.setText(currentMarkdown);
        renderMarkdown(currentMarkdown);
    }

    public String getMarkdown() {
        return currentMarkdown;
    }

    private void updateViewMode() {
        if (toggleRawBtn.isSelected()) {
            toggleRawBtn.setText(I18n.getOrDefault("markdown.btn.toggle_formatted", "👁️ Vue Mise en Page"));
            setCenter(rawTextArea);
        } else {
            toggleRawBtn.setText(I18n.getOrDefault("markdown.btn.toggle_raw", "📝 Code Brut"));
            setCenter(scrollPane);
        }
    }

    private void copyToClipboard() {
        if (currentMarkdown != null && !currentMarkdown.isBlank()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(currentMarkdown);
            clipboard.setContent(content);

            String oldText = copyBtn.getText();
            copyBtn.setText(I18n.getOrDefault("markdown.btn.copied", "✅ Copié !"));
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            pause.setOnFinished(e -> copyBtn.setText(oldText));
            pause.play();
        }
    }

    private void renderMarkdown(String markdown) {
        formattedContainer.getChildren().clear();

        if (markdown == null || markdown.isBlank()) {
            Label emptyLbl = new Label(I18n.getOrDefault("markdown.empty", "Aucun rapport à afficher."));
            emptyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
            formattedContainer.getChildren().add(emptyLbl);
            return;
        }

        String[] lines = markdown.split("\\r?\\n");
        List<String> tableBuffer = new ArrayList<>();
        boolean inTable = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                tableBuffer.add(trimmed);
                inTable = true;
                continue;
            } else if (inTable) {
                // Table ended, process table buffer
                formattedContainer.getChildren().add(buildTableNode(tableBuffer));
                tableBuffer.clear();
                inTable = false;
            }

            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.startsWith("# ")) {
                Label h1 = new Label(trimmed.substring(2));
                h1.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 8 0 4 0; -fx-border-color: transparent transparent rgba(56, 189, 248, 0.4) transparent; -fx-border-width: 0 0 2 0;");
                h1.setWrapText(true);
                formattedContainer.getChildren().add(h1);
            } else if (trimmed.startsWith("## ")) {
                Label h2 = new Label(trimmed.substring(3));
                h2.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #60a5fa; -fx-padding: 6 0 2 0;");
                h2.setWrapText(true);
                formattedContainer.getChildren().add(h2);
            } else if (trimmed.startsWith("### ")) {
                Label h3 = new Label(trimmed.substring(4));
                h3.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #93c5fd; -fx-padding: 4 0 2 0;");
                h3.setWrapText(true);
                formattedContainer.getChildren().add(h3);
            } else if (trimmed.startsWith("---") || trimmed.startsWith("***")) {
                Separator sep = new Separator();
                sep.setStyle("-fx-opacity: 0.3; -fx-padding: 4 0;");
                formattedContainer.getChildren().add(sep);
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
                String content = trimmed.substring(2);
                HBox itemBox = new HBox(6);
                itemBox.setAlignment(Pos.TOP_LEFT);
                Label bullet = new Label("•");
                bullet.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 14px;");
                TextFlow tf = parseFormattedTextFlow(content);
                HBox.setHgrow(tf, Priority.ALWAYS);
                itemBox.getChildren().addAll(bullet, tf);
                formattedContainer.getChildren().add(itemBox);
            } else {
                TextFlow paragraph = parseFormattedTextFlow(line);
                formattedContainer.getChildren().add(paragraph);
            }
        }

        if (inTable && !tableBuffer.isEmpty()) {
            formattedContainer.getChildren().add(buildTableNode(tableBuffer));
            tableBuffer.clear();
        }
    }

    private Node buildTableNode(List<String> tableLines) {
        if (tableLines == null || tableLines.isEmpty()) return new VBox();

        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setStyle("-fx-background-color: #334155; -fx-padding: 1; -fx-background-radius: 4;");

        int rowIndex = 0;
        for (String line : tableLines) {
            // Skip markdown separator row like | :--- | :--- |
            if (line.matches("^\\|[\\s\\-:]+(\\|[\\s\\-:]+)+\\|$")) {
                continue;
            }

            String[] rawCells = line.split("\\|");
            List<String> cells = new ArrayList<>();
            for (String cell : rawCells) {
                if (!cell.isBlank() || cells.size() > 0) {
                    cells.add(cell.trim());
                }
            }
            if (cells.isEmpty()) continue;
            // Trim trailing empty element if split started/ended with pipes
            if (!cells.isEmpty() && cells.get(0).isEmpty()) cells.remove(0);

            boolean isHeader = (rowIndex == 0);

            for (int col = 0; col < cells.size(); col++) {
                String cellText = cells.get(col);
                StackPane cellBox = new StackPane();
                cellBox.setPadding(new Insets(5, 8, 5, 8));

                if (isHeader) {
                    cellBox.setStyle("-fx-background-color: #1e293b;");
                    TextFlow tf = parseFormattedTextFlow(cellText);
                    for (Node n : tf.getChildren()) {
                        if (n instanceof Text t) {
                            t.setFont(Font.font("System", FontWeight.BOLD, 12));
                            t.setStyle("-fx-fill: #f8fafc;");
                        }
                    }
                    cellBox.getChildren().add(tf);
                } else {
                    String bg = (rowIndex % 2 == 0) ? "rgba(30, 41, 59, 0.7)" : "rgba(15, 23, 42, 0.85)";
                    cellBox.setStyle("-fx-background-color: " + bg + ";");
                    TextFlow tf = parseFormattedTextFlow(cellText);
                    cellBox.getChildren().add(tf);
                }

                GridPane.setHgrow(cellBox, Priority.ALWAYS);
                grid.add(cellBox, col, rowIndex);
            }
            rowIndex++;
        }

        return grid;
    }

    private TextFlow parseFormattedTextFlow(String text) {
        TextFlow flow = new TextFlow();
        if (text == null || text.isBlank()) return flow;

        // Apply LaTeX Unicode transformations for equations
        String processed = LaTeXFormatter.formatLaTeX(text);

        // Parse inline tokens: **bold**, `code`, normal text
        int i = 0;
        int len = processed.length();

        while (i < len) {
            if (processed.startsWith("**", i)) {
                int end = processed.indexOf("**", i + 2);
                if (end != -1) {
                    String boldContent = processed.substring(i + 2, end);
                    Text boldText = new Text(boldContent);
                    boldText.setFont(Font.font("System", FontWeight.BOLD, 12));
                    boldText.setStyle("-fx-fill: #f1f5f9;");
                    flow.getChildren().add(boldText);
                    i = end + 2;
                    continue;
                }
            } else if (processed.charAt(i) == '`') {
                int end = processed.indexOf('`', i + 1);
                if (end != -1) {
                    String codeContent = processed.substring(i + 1, end);
                    Label codeBadge = new Label(codeContent);
                    codeBadge.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-background-color: rgba(30, 41, 59, 0.9); -fx-text-fill: #38bdf8; -fx-padding: 1 4; -fx-background-radius: 3; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 3;");
                    flow.getChildren().add(codeBadge);
                    i = end + 1;
                    continue;
                }
            } else if (processed.charAt(i) == '*' && i + 1 < len && processed.charAt(i + 1) != ' ') {
                int end = processed.indexOf('*', i + 1);
                if (end != -1) {
                    String italicContent = processed.substring(i + 1, end);
                    Text italicText = new Text(italicContent);
                    italicText.setFont(Font.font("System", FontPosture.ITALIC, 12));
                    italicText.setStyle("-fx-fill: #cbd5e1;");
                    flow.getChildren().add(italicText);
                    i = end + 1;
                    continue;
                }
            }

            // Normal text segment until next formatting token
            int nextSpecial = len;
            int nextBold = processed.indexOf("**", i);
            int nextCode = processed.indexOf('`', i);
            int nextItalic = processed.indexOf('*', i);

            if (nextBold != -1 && nextBold < nextSpecial) nextSpecial = nextBold;
            if (nextCode != -1 && nextCode < nextSpecial) nextSpecial = nextCode;
            if (nextItalic != -1 && nextItalic < nextSpecial) nextSpecial = nextItalic;

            String plain = processed.substring(i, nextSpecial);
            Text plainText = new Text(plain);
            plainText.setFont(Font.font("System", 12));
            plainText.setStyle("-fx-fill: #e2e8f0;");
            flow.getChildren().add(plainText);
            i = nextSpecial;
        }

        return flow;
    }
}
