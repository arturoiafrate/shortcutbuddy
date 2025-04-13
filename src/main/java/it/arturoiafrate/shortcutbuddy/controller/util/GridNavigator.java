package it.arturoiafrate.shortcutbuddy.controller.util;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import it.arturoiafrate.shortcutbuddy.model.bean.Shortcut;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class GridNavigator {
    private final GridPane gridPane;
    private final ScrollPane scrollPane;
    private final String defaultStyleClass;
    private final String selectedStyleClass;

    private int currentRow;
    private int currentCol;
    private int itemCount;
    private int numColumns;
    private Node selectedNode;

    public GridNavigator(GridPane gridPane, ScrollPane scrollPane, String defaultStyleClass, String selectedStyleClass) {
        this.gridPane = Objects.requireNonNull(gridPane, "GridPane non può essere null");
        this.scrollPane = Objects.requireNonNull(scrollPane, "ScrollPane non può essere null");
        this.defaultStyleClass = Objects.requireNonNull(defaultStyleClass, "DefaultStyleClass non può essere null");
        this.selectedStyleClass = Objects.requireNonNull(selectedStyleClass, "SelectedStyleClass non può essere null");
        resetSelection();
        this.itemCount = 0;
        this.numColumns = 1;
    }

    public void setGridData(int itemCount, int columns) {
        this.itemCount = itemCount;
        this.numColumns = Math.max(1, columns);
        resetSelection();
    }

    public void resetSelection() {
        if (selectedNode != null) {
            selectedNode.setStyle(defaultStyleClass);
        }
        this.currentRow = -1;
        this.currentCol = -1;
        selectedNode = null;
        if (scrollPane != null) {
            scrollPane.setVvalue(0);
        }
    }

    public void navigate(int keyCode) {
        if (itemCount == 0) return;
        int newRow = currentRow;
        int newCol = currentCol;
        boolean navigated = false;
        if (currentRow == -1 || currentCol == -1) {
            if (keyCode == NativeKeyEvent.VC_DOWN || keyCode == NativeKeyEvent.VC_RIGHT) {
                newRow = 0;
                newCol = 0;
                navigated = true;
            } else if (keyCode == NativeKeyEvent.VC_UP || keyCode == NativeKeyEvent.VC_LEFT) {
                int lastIndex = itemCount - 1;
                newRow = lastIndex / numColumns;
                newCol = lastIndex % numColumns;
                navigated = true;
            }
        } else {
            switch (keyCode) {
                case NativeKeyEvent.VC_UP:    newRow--; navigated = true; break;
                case NativeKeyEvent.VC_DOWN:  newRow++; navigated = true; break;
                case NativeKeyEvent.VC_LEFT:
                    newCol--;
                    if (newCol < 0 && newRow > 0) { newRow--; newCol = numColumns - 1; }
                    else if (newCol < 0) newCol = 0;
                    navigated = true;
                    break;
                case NativeKeyEvent.VC_RIGHT:
                    newCol++;
                    if (newCol >= numColumns && (currentRow * numColumns + currentCol + 1 < itemCount)) { newRow++; newCol = 0; }
                    else if (newCol >= numColumns) newCol = numColumns - 1;
                    navigated = true;
                    break;
            }
        }

        if (navigated) {
            validateAndUpdateSelection(newRow, newCol);
        }
    }

    public Optional<Shortcut> getSelectedShortcut(List<Shortcut> currentList) {
        if (currentRow == -1 || currentCol == -1 || currentList == null) {
            return Optional.empty();
        }
        int index = currentRow * numColumns + currentCol;
        if (index >= 0 && index < currentList.size()) {
            return Optional.of(currentList.get(index));
        }
        return Optional.empty();
    }

    public boolean isSomethingSelected() {
        return currentRow != -1 && currentCol != -1;
    }


    private void validateAndUpdateSelection(int newRow, int newCol) {
        if (newRow >= 0 && newCol >= 0 && newCol < numColumns) {
            int newIndex = newRow * numColumns + newCol;
            if (newIndex >= 0 && newIndex < itemCount) {
                updateSelection(newRow, newCol, true);
            }
        }
    }


    private void ensureNodeVisible(Node node) {
        if (scrollPane == null || node == null || gridPane == null) return;
        Bounds nodeBoundsInGrid = node.getBoundsInParent();
        Bounds gridBoundsInScroll = gridPane.getLayoutBounds();
        double nodeY = nodeBoundsInGrid.getMinY();
        double nodeHeight = nodeBoundsInGrid.getHeight();
        double gridHeight = gridBoundsInScroll.getHeight();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        double currentVvalue = scrollPane.getVvalue();
        double maxScrollOffset = Math.max(0, gridHeight - viewportHeight);
        double visibleTop = currentVvalue * maxScrollOffset;
        double visibleBottom = visibleTop + viewportHeight;
        double targetVvalue = currentVvalue;

        if (nodeY < visibleTop) {
            targetVvalue = nodeY / maxScrollOffset;
        } else if (nodeY + nodeHeight > visibleBottom) {
            targetVvalue = (nodeY + nodeHeight - viewportHeight) / maxScrollOffset;
        }

        if (gridHeight > viewportHeight && targetVvalue != currentVvalue) {
            targetVvalue = Math.max(0, Math.min(1, targetVvalue));
            scrollPane.setVvalue(targetVvalue);
        }
    }


    private Node getNodeByRowColumn(int row, int col) {
        for (Node node : gridPane.getChildren()) {
            Integer rowIndex = GridPane.getRowIndex(node);
            Integer colIndex = GridPane.getColumnIndex(node);
            int r = (rowIndex == null) ? 0 : rowIndex;
            int c = (colIndex == null) ? 0 : colIndex;
            if (r == row && c == col) {
                return node;
            }
        }
        return null;
    }

    private void updateSelection(int newRow, int newCol, boolean scroll) {
        Node newNode = getNodeByRowColumn(newRow, newCol);
        if (newNode != null) {
            if (selectedNode != null) {
                selectedNode.setStyle(defaultStyleClass);
            }
            currentRow = newRow;
            currentCol = newCol;
            selectedNode = newNode;
            selectedNode.setStyle(selectedStyleClass);
            selectedNode.requestFocus();
            if (scroll) {
                ensureNodeVisible(selectedNode);
            }
        }
    }

}
