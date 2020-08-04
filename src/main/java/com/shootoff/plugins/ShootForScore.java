/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2016 phrack
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shootoff.plugins;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import com.shootoff.camera.Shot;
import com.shootoff.camera.shot.ShotColor;
import com.shootoff.config.DynamicGlobal;
import com.shootoff.gui.targets.TargetView;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;
import com.shootoff.util.SwingFXUtils;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class ShootForScore extends TrainingExerciseBase implements TrainingExercise {
    private final static String POINTS_COL_NAME = "Score";
    private final static int POINTS_COL_WIDTH = 60;

    private int redScore = 0;
    private int greenScore = 0;
    private int hitsCnt = 0;
    private double minX = Double.MAX_VALUE, maxX = 0, minY = Double.MAX_VALUE, maxY = 0;
    private double targetX = 1;

    private final ChoiceBox<Integer> maxHitsChoiceBox = new ChoiceBox<>();
    private final CheckBox autoResetCheckBox = new CheckBox();
    private final CheckBox autoSaveFeedBox = new CheckBox();
    private final TextField targetXSizeCM = new TextField();
    private final TextField scaleFactor = new TextField();

    public ShootForScore() {
    }

    public ShootForScore(List<Target> targets) {
        super(targets);
    }

    @Override
    public void init() {
        super.addShotTimerColumn(POINTS_COL_NAME, POINTS_COL_WIDTH);
        initializeGui();
    }

    private void initializeGui() {
        final GridPane exercisePane = new GridPane();

        exercisePane.add(new Label("Maximum Hits"), 0, 0);
        exercisePane.add(maxHitsChoiceBox, 1, 0);

        maxHitsChoiceBox.getItems().add(5);
        maxHitsChoiceBox.getItems().add(10);
        maxHitsChoiceBox.getItems().add(20);

        maxHitsChoiceBox.getSelectionModel().select(0);

        exercisePane.add(new Label("Auto-Reset after max hits"), 0, 1);
        exercisePane.add(autoResetCheckBox, 1, 1);
        autoResetCheckBox.setSelected(false);

        exercisePane.add(new Label("Auto-Save feed image after max hits"), 0, 2);
        exercisePane.add(autoSaveFeedBox, 1, 2);
        autoSaveFeedBox.setSelected(true);

        exercisePane.add(new Label("Target physical X Size (cm)"), 0, 3);
        exercisePane.add(targetXSizeCM, 1, 3);
        // Make the text field Numeric only
        targetXSizeCM.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d{0,7}([\\.]\\d{0,4})?")) {
                    targetXSizeCM.setText(oldValue);
                }
            }
        });
        // targetXSizeCM.setText("50"); // ISSF 25m RAPID FIRE PISTOL
        targetXSizeCM.setText("18"); // Homeless 5_25 Pistol Target Size

        exercisePane.add(new Label("Training Scale (Simulated Distance/Real Distance)"), 0, 4);
        exercisePane.add(scaleFactor, 1, 4);
        // Make the text field Numeric only
        scaleFactor.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d{0,7}([\\.]\\d{0,4})?")) {
                    scaleFactor.setText(oldValue);
                }
            }
        });
        scaleFactor.setText("5"); // Shoot at 5M for 25M target

        addExercisePane(exercisePane);
    }

    @Override
    public void targetUpdate(Target target, TargetChange change) {
    }

    /**
     * Returns the score for the red player. This method exists to make this
     * exercise easier to test.
     *
     * @return red's score
     */
    protected int getRedScore() {
        return redScore;
    }

    /**
     * Returns the score for the green player. This method exists to make this
     * exercise easier to test.
     *
     * @return green's score
     */
    protected int getGreenScore() {
        return greenScore;
    }

    @Override
    public ExerciseMetadata getInfo() {
        return new ExerciseMetadata("Shoot for Score", "1.0", "phrack",
                "This exercise works with targets that have score tags "
                        + "assigned to regions. Any time a target region is hit, "
                        + "the number of points assigned to that region are added " + "to your total score.");
    }

    @Override
    public void shotListener(Shot shot, Optional<Hit> hit) {
        if (!hit.isPresent())
            return;
        hitsCnt++;

        final TargetRegion r = hit.get().getHitRegion();

        if (r.tagExists("points")) {
            super.setShotTimerColumnText(POINTS_COL_NAME, r.getTag("points"));
            if (shot.getColor().equals(ShotColor.RED) || shot.getColor().equals(ShotColor.INFRARED)) {
                redScore += Integer.parseInt(r.getTag("points"));
            } else if (shot.getColor().equals(ShotColor.GREEN)) {
                greenScore += Integer.parseInt(r.getTag("points"));
            }
        }

        String message = "score: 0";

        if (redScore > 0 && greenScore > 0) {
            message = String.format("red score: %d%ngreen score: %d", redScore, greenScore);
        }

        if (redScore > 0 && greenScore > 0) {
            message = String.format("red score: %d%ngreen score: %d", redScore, greenScore);
        } else if (redScore > 0) {
            message = String.format("red score: %d", redScore);
        } else if (greenScore > 0) {
            message = String.format("green score: %d", greenScore);
        }

        super.showTextOnFeed(message);

        // Calculate the angle for shot
        int clock = 0;
        final Shot s = hit.get().getShot();
        final Target t = hit.get().getTarget();
        if (s != null && t != null) {
            // Target 0,0 = Shot 290,250
            double sx = s.getX(), sy = s.getY();
            minX = Math.min(minX, sx);
            maxX = Math.max(maxX, sx);
            minY = Math.min(minY, sx);
            maxY = Math.max(maxY, sx);
            double tx = t.getPosition().getX() + 290.0, ty = t.getPosition().getY() + 250.0;
            targetX = t.getDimension().getWidth();
            float angle = (float) Math.toDegrees(Math.atan2(sy - ty, sx - tx));
            if (angle < 0) {
                angle += 360;
            }
            clock = ((int) (angle + 15) / 30 + 3) % 12;
            if (clock == 0)
                clock = 12;
        }

        if (r.tagExists("points")) {
            // Voice the current shot score
            TextToSpeech.say(r.getTag("points") + " " + clock + " clock");
        }

        if (autoResetCheckBox.isSelected() && hitsCnt >= maxHitsChoiceBox.getValue()) {
            try {
                double spread = getMOA();
                if (autoSaveFeedBox.isSelected()) {
                    Platform.runLater(() -> {
                        // Hide the target for feed snapshot
                        ((TargetView) t).getTargetGroup().setVisible(false);
                        final Node container = DynamicGlobal.controller.getSelectedCameraContainer();
                        final RenderedImage renderedImage = SwingFXUtils
                                .fromFXImage(container.snapshot(new SnapshotParameters(), null), null);
                        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss-").format(new java.util.Date());
                        String feedFile = System.getProperty("shootoff.home") + File.separator + "shootlog"
                                + File.separator + timeStamp + String.format("%d-%.2f", redScore, spread) + ".png";
                        File imageFile = new File(feedFile);
                        imageFile.getParentFile().mkdirs();
                        try {
                            ImageIO.write(renderedImage, "png", imageFile);
                        } catch (final IOException e) {
                        }
                        // Show the target after feed snapshot
                        ((TargetView) t).getTargetGroup().setVisible(true);
                    });
                }
                Thread.sleep(1000);
                TextToSpeech.say(
                        String.format("Total score %d, spread %.2f inches, reset in three seconds.", redScore, spread));
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            super.reset();
        }
    }

    private double getMOA() {
        double max = Math.max(maxX - minX, maxY - minY);
        double cms = max * Double.parseDouble(targetXSizeCM.getText()) / targetX;
        double inches = cms * Double.parseDouble(scaleFactor.getText()) / 2.54;
        return inches;
    }

    @Override
    public void reset(List<Target> targets) {
        redScore = 0;
        greenScore = 0;
        hitsCnt = 0;
        minX = maxX = minY = maxY = 0;
        targetX = 1;
        super.showTextOnFeed("score: 0");
    }

    @Override
    public void destroy() {
        super.destroy();
    }

}
