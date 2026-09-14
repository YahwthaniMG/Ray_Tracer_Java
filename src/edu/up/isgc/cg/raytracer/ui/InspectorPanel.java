package edu.up.isgc.cg.raytracer.ui;

import edu.up.isgc.cg.raytracer.math.Vector3D;
import edu.up.isgc.cg.raytracer.objects.Material;
import edu.up.isgc.cg.raytracer.tools.SceneProperties;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * Right-hand panel: shows the name/type of whatever is currently selected in the
 * viewport, with fields to move/rotate/scale/aim it depending on what kind of thing it
 * is (see {@link SceneProperties} for which fields apply to which type). This is the
 * numeric-field escape hatch for whatever a drag in the viewport can't reach directly —
 * see {@link ViewportPanel} for click-to-select and Top/Front/Side drag-to-move.
 *
 * @author Claude (Anthropic)
 */
public class InspectorPanel extends JPanel {

    private static final int WIDTH = 220;

    private final JTextField nameField = new JTextField();
    private final JLabel typeLabel = new JLabel();
    private final JButton deleteButton = new JButton("Delete");

    private final JPanel fieldsPanel = new JPanel();
    private final JLabel emptyLabel = new JLabel(
            "<html>No selection.<br>Click an object, camera or light in the viewport.</html>");

    private final JSpinner posX = numberSpinner(0.1);
    private final JSpinner posY = numberSpinner(0.1);
    private final JSpinner posZ = numberSpinner(0.1);

    private final JComponent colorSection;
    private final JButton colorSwatch = new JButton();

    private final JComponent rotationSection;
    private final JSpinner rotX = numberSpinner(1.0);
    private final JSpinner rotY = numberSpinner(1.0);
    private final JSpinner rotZ = numberSpinner(1.0);

    private final JComponent scaleSection;
    private final JSpinner scaleField = numberSpinner(0.1);

    private final JComponent aimSection;
    private final JSpinner yawField = numberSpinner(1.0);
    private final JSpinner pitchField = numberSpinner(1.0);

    private final JComponent fovSection;
    private final JSpinner fovField = numberSpinner(1.0);

    private final JComponent intensitySection;
    private final JSpinner intensityField = numberSpinner(0.5);

    private final JComponent coneSection;
    private final JSpinner coneAngleField = numberSpinner(1.0);
    private final JSpinner penumbraField = numberSpinner(1.0);

    private final JComponent materialSection;
    private final JSpinner ambientField = numberSpinner(0.05);
    private final JSpinner diffuseField = numberSpinner(0.05);
    private final JSpinner specularField = numberSpinner(0.05);
    private final JSpinner shininessField = numberSpinner(5.0);
    private final JSpinner reflectivityField = numberSpinner(0.05);
    private final JSpinner transparencyField = numberSpinner(0.05);
    private final JSpinner refractiveIndexField = numberSpinner(0.05);

    private Object selected;
    private Runnable onChange;
    private Runnable onDeleteRequested;
    private boolean updatingFromSelection;

    public InspectorPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.BG_PANEL);
        setBorder(new MatteBorder(0, 1, 0, 0, Theme.BORDER));

        colorSection = section("Color", colorRow());
        rotationSection = section("Rotation (deg)", axisRow("X", rotX), axisRow("Y", rotY), axisRow("Z", rotZ));
        scaleSection = section("Scale", axisRow("×", scaleField));
        aimSection = section("Aim (deg)", axisRow("Yaw", yawField), axisRow("Pitch", pitchField));
        fovSection = section("Field of View (deg)", axisRow("FOV", fovField));
        intensitySection = section("Intensity", axisRow("×", intensityField));
        coneSection = section("Cone (deg)", axisRow("Angle", coneAngleField), axisRow("Penumbra", penumbraField));
        materialSection = section("Material",
                axisRow("Ambient", ambientField), axisRow("Diffuse", diffuseField),
                axisRow("Specular", specularField), axisRow("Shininess", shininessField),
                axisRow("Reflectivity", reflectivityField), axisRow("Transparency", transparencyField),
                axisRow("Refract. IOR", refractiveIndexField), materialPresetsRow());

        add(header());
        add(buildFieldsPanel());
        add(buildEmptyLabel());
        add(Box.createVerticalGlue());

        fieldsPanel.setVisible(false);

        ChangeListener onEdit = e -> onFieldsEdited();
        for (JSpinner s : new JSpinner[]{posX, posY, posZ, rotX, rotY, rotZ, scaleField, yawField, pitchField, fovField,
                intensityField, coneAngleField, penumbraField,
                ambientField, diffuseField, specularField, shininessField, reflectivityField, transparencyField, refractiveIndexField}) {
            s.addChangeListener(onEdit);
        }

        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onNameEdited(); }
            @Override public void removeUpdate(DocumentEvent e) { onNameEdited(); }
            @Override public void changedUpdate(DocumentEvent e) { onNameEdited(); }
        });
    }

    /**
     * Fixes the width but leaves height to whatever the sections actually need — an item
     * with every section visible at once (e.g. a fully-featured object with Material)
     * routinely needs more vertical space than the window has, so {@link EditorWindow}
     * wraps this in a scroll pane; that only works if this reports its true natural
     * height instead of a hardcoded one (the same fix {@link SidebarPanel} already needed).
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension natural = super.getPreferredSize();
        return new Dimension(WIDTH, natural.height);
    }

    /** Called after any field edit so the viewport can repaint to reflect it. */
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    /** Called when the Delete button is pressed with something selected — the caller owns the {@code Scene}. */
    public void setOnDeleteRequested(Runnable onDeleteRequested) {
        this.onDeleteRequested = onDeleteRequested;
    }

    /** Wired to {@link ViewportPanel#setOnSelectionChanged}: refreshes the shown fields. */
    public void setSelected(Object item) {
        this.selected = item;
        boolean hasSelection = item != null;
        fieldsPanel.setVisible(hasSelection);
        emptyLabel.setVisible(!hasSelection);
        if (hasSelection) {
            typeLabel.setText(SceneProperties.getTypeLabel(item));
            pushFields(item); // also pushes the name, guarded against feeding back into onNameEdited
        }
        revalidate();
        repaint();
    }

    private JComponent header() {
        JLabel header = new JLabel("Inspector");
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setForeground(Theme.TEXT);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(12, 14, 12, 14)));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    private JComponent buildFieldsPanel() {
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBackground(Theme.BG_PANEL);
        fieldsPanel.setBorder(new EmptyBorder(12, 14, 12, 14));
        fieldsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        nameField.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameField.setForeground(Theme.TEXT);
        nameField.setBackground(Theme.BG_PANEL);
        nameField.setCaretColor(Theme.TEXT);
        nameField.setBorder(null);
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameField.setMaximumSize(new Dimension(WIDTH, 26));
        nameField.setToolTipText("Rename");

        typeLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        typeLabel.setForeground(Theme.MUTED);

        deleteButton.setFont(new Font("SansSerif", Font.PLAIN, 11));
        deleteButton.setFocusPainted(false);
        deleteButton.setMargin(new Insets(1, 6, 1, 6));
        deleteButton.setToolTipText("Remove this from the scene");
        deleteButton.addActionListener(e -> onDeleteClicked());

        JPanel typeRow = new JPanel(new BorderLayout());
        typeRow.setOpaque(false);
        typeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        typeRow.setMaximumSize(new Dimension(WIDTH, 24));
        typeRow.add(typeLabel, BorderLayout.WEST);
        typeRow.add(deleteButton, BorderLayout.EAST);

        fieldsPanel.add(nameField);
        fieldsPanel.add(Box.createVerticalStrut(4));
        fieldsPanel.add(typeRow);
        fieldsPanel.add(section("Position", axisRow("X", posX), axisRow("Y", posY), axisRow("Z", posZ)));
        fieldsPanel.add(colorSection);
        fieldsPanel.add(intensitySection);
        fieldsPanel.add(coneSection);
        fieldsPanel.add(rotationSection);
        fieldsPanel.add(scaleSection);
        fieldsPanel.add(aimSection);
        fieldsPanel.add(fovSection);
        fieldsPanel.add(materialSection);
        return fieldsPanel;
    }

    private JComponent colorRow() {
        colorSwatch.setPreferredSize(new Dimension(44, 22));
        colorSwatch.setMaximumSize(new Dimension(44, 22));
        colorSwatch.setOpaque(true);
        colorSwatch.setFocusPainted(false);
        colorSwatch.setBorder(new LineBorder(Theme.BORDER, 1));
        colorSwatch.setToolTipText("Click to change color");
        colorSwatch.addActionListener(e -> onColorClicked());

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(colorSwatch);
        return row;
    }

    private void onDeleteClicked() {
        if (selected == null || onDeleteRequested == null) return;
        onDeleteRequested.run();
    }

    private void onColorClicked() {
        if (selected == null) return;
        Color current = SceneProperties.getColor(selected);
        Color picked = JColorChooser.showDialog(this, "Choose color", current);
        if (picked == null) return;
        SceneProperties.setColor(selected, picked);
        colorSwatch.setBackground(picked);
        if (onChange != null) onChange.run();
    }

    private JComponent buildEmptyLabel() {
        emptyLabel.setForeground(Theme.MUTED);
        emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        emptyLabel.setBorder(new EmptyBorder(14, 14, 14, 14));
        emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return emptyLabel;
    }

    private JComponent section(String title, JComponent... rows) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        titleLabel.setForeground(Theme.MUTED);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Right padding: same Nimbus paint-time-vs-layout-time glyph measurement mismatch
        // documented on SidebarPanel's section titles, which can clip a tight-fit label's
        // last character by a pixel or two.
        titleLabel.setBorder(new EmptyBorder(14, 0, 4, 6));
        panel.add(titleLabel);

        for (JComponent row : rows) panel.add(row);
        return panel;
    }

    /**
     * A "label: spinner" row. The label column is wide enough for the longest label this
     * panel actually uses ("Reflectivity", "Refract. IOR") — a narrower fixed width here
     * (28px was the original) silently truncates anything longer than "Yaw" instead of
     * wrapping or growing, which is how "Pitch"/"Angle"/"Penumbra" ended up clipped to
     * "Pit…"/"An…"/"Pe…" before this was widened.
     */
    private JComponent axisRow(String axisLabel, JSpinner spinner) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(axisLabel);
        label.setForeground(Theme.MUTED);
        label.setPreferredSize(new Dimension(84, 20));

        row.add(label);
        row.add(spinner);
        return row;
    }

    /**
     * Quick material presets so getting a realistic-looking mirror or glass doesn't mean
     * hand-tuning several spinners — Mirror/Glass set a moderate reflectivity/transparency
     * (not 1.0: two facing surfaces at full strength wash out to white, since this
     * raytracer has no environment light to otherwise ground the reflection/refraction —
     * see the render bar's Mode combo for a "make everything a mirror" preview instead).
     */
    private JComponent materialPresetsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(presetButton("Mirror", m -> {
            m.setReflectivity(0.5);
            m.setTransparency(0.0);
        }));
        row.add(presetButton("Glass", m -> {
            m.setTransparency(0.85);
            m.setReflectivity(0.05);
            m.setRefractiveIndex(1.5);
        }));
        row.add(presetButton("Matte", m -> {
            m.setReflectivity(0.0);
            m.setTransparency(0.0);
        }));
        return row;
    }

    private JButton presetButton(String label, java.util.function.Consumer<Material> apply) {
        JButton button = new JButton(label);
        button.setFont(new Font("SansSerif", Font.PLAIN, 11));
        button.setFocusPainted(false);
        button.setMargin(new Insets(1, 6, 1, 6));
        button.addActionListener(e -> onMaterialPresetClicked(apply));
        return button;
    }

    private void onMaterialPresetClicked(java.util.function.Consumer<Material> apply) {
        if (selected == null || !SceneProperties.supportsMaterial(selected)) return;
        apply.accept(SceneProperties.getMaterial(selected));
        pushFields(selected); // refreshes every spinner, guarded against feeding back into onFieldsEdited
        if (onChange != null) onChange.run();
    }

    private static JSpinner numberSpinner(double step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0.0, -100000.0, 100000.0, step));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, step < 1 ? "0.00" : "0.#"));
        Dimension size = new Dimension(70, 24);
        spinner.setPreferredSize(size);
        spinner.setMaximumSize(size);
        return spinner;
    }

    private void pushFields(Object item) {
        updatingFromSelection = true;
        try {
            nameField.setText(SceneProperties.getName(item));

            Vector3D pos = SceneProperties.getPosition(item);
            if (pos != null) {
                posX.setValue(round(pos.getX()));
                posY.setValue(round(pos.getY()));
                posZ.setValue(round(pos.getZ()));
            }

            colorSection.setVisible(SceneProperties.supportsColor(item));
            if (SceneProperties.supportsColor(item)) {
                colorSwatch.setBackground(SceneProperties.getColor(item));
            }

            rotationSection.setVisible(SceneProperties.supportsRotation(item));
            if (SceneProperties.supportsRotation(item)) {
                Vector3D rot = SceneProperties.getRotationDegrees(item);
                rotX.setValue(round(rot.getX()));
                rotY.setValue(round(rot.getY()));
                rotZ.setValue(round(rot.getZ()));
            }

            scaleSection.setVisible(SceneProperties.supportsScale(item));
            if (SceneProperties.supportsScale(item)) {
                scaleField.setValue(round(SceneProperties.getScale(item)));
            }

            aimSection.setVisible(SceneProperties.supportsAim(item));
            if (SceneProperties.supportsAim(item)) {
                double[] aim = SceneProperties.getAimDegrees(item);
                yawField.setValue(round(aim[0]));
                pitchField.setValue(round(aim[1]));
            }

            fovSection.setVisible(SceneProperties.supportsFov(item));
            if (SceneProperties.supportsFov(item)) {
                fovField.setValue(round(SceneProperties.getFovDegrees(item)));
            }

            intensitySection.setVisible(SceneProperties.supportsIntensity(item));
            if (SceneProperties.supportsIntensity(item)) {
                intensityField.setValue(round(SceneProperties.getIntensity(item)));
            }

            coneSection.setVisible(SceneProperties.supportsCone(item));
            if (SceneProperties.supportsCone(item)) {
                coneAngleField.setValue(round(SceneProperties.getConeAngleDegrees(item)));
                penumbraField.setValue(round(SceneProperties.getPenumbraDegrees(item)));
            }

            materialSection.setVisible(SceneProperties.supportsMaterial(item));
            if (SceneProperties.supportsMaterial(item)) {
                Material material = SceneProperties.getMaterial(item);
                ambientField.setValue(round(material.getAmbient()));
                diffuseField.setValue(round(material.getDiffuse()));
                specularField.setValue(round(material.getSpecular()));
                shininessField.setValue(round(material.getShininess()));
                reflectivityField.setValue(round(material.getReflectivity()));
                transparencyField.setValue(round(material.getTransparency()));
                refractiveIndexField.setValue(round(material.getRefractiveIndex()));
            }
        } finally {
            updatingFromSelection = false;
        }
    }

    private void onNameEdited() {
        if (updatingFromSelection || selected == null) return;
        SceneProperties.setName(selected, nameField.getText());
        if (onChange != null) onChange.run();
    }

    private void onFieldsEdited() {
        if (updatingFromSelection || selected == null) return;
        SceneProperties.setPosition(selected, new Vector3D(value(posX), value(posY), value(posZ)));
        if (SceneProperties.supportsRotation(selected)) {
            SceneProperties.setRotationDegrees(selected, new Vector3D(value(rotX), value(rotY), value(rotZ)));
        }
        if (SceneProperties.supportsScale(selected)) {
            SceneProperties.setScale(selected, value(scaleField));
        }
        if (SceneProperties.supportsAim(selected)) {
            SceneProperties.setAimDegrees(selected, value(yawField), value(pitchField));
        }
        if (SceneProperties.supportsFov(selected)) {
            SceneProperties.setFovDegrees(selected, value(fovField));
        }
        if (SceneProperties.supportsIntensity(selected)) {
            SceneProperties.setIntensity(selected, value(intensityField));
        }
        if (SceneProperties.supportsCone(selected)) {
            SceneProperties.setConeAngleDegrees(selected, value(coneAngleField));
            SceneProperties.setPenumbraDegrees(selected, value(penumbraField));
        }
        if (SceneProperties.supportsMaterial(selected)) {
            Material material = SceneProperties.getMaterial(selected);
            material.setAmbient(value(ambientField));
            material.setDiffuse(value(diffuseField));
            material.setSpecular(value(specularField));
            material.setShininess(value(shininessField));
            material.setReflectivity(value(reflectivityField));
            material.setTransparency(value(transparencyField));
            material.setRefractiveIndex(value(refractiveIndexField));
        }
        if (onChange != null) onChange.run();
    }

    private static double value(JSpinner spinner) {
        return ((Number) spinner.getValue()).doubleValue();
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
