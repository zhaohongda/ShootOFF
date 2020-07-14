package com.shootoff.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.shootoff.camera.Shot;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;
import com.shootoff.targets.TargetRegion;

import javafx.geometry.BoundingBox;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ShootOut extends ProjectorTrainingExerciseBase implements TrainingExercise {
    private static final int MIN_TARGET_VISIBLE = 25;
    private static final int DEFAULT_WIDTH = 640;
    private static final int DEFAULT_HEIGHT = 480;

    private final List<File> acceptedTargets = Arrays.asList(new File("targets/Swedish_Soldier.target"),
            new File("targets/AQT_Silhouette.target"), new File("targets/USPSA.target"));
    // ,new File("targets/IPSC.target"));

    private final List<String> courses = new ArrayList<>();
    private final Map<String, List<ClipArea>> courseBoundingBoxes = new HashMap<>();
    private final ChoiceBox<Integer> maxTargetsChoiceBox = new ChoiceBox<>();
    private final ChoiceBox<Integer> requiredHitsChoiceBox = new ChoiceBox<>();
    private final CheckBox injectDontShootCheckBox = new CheckBox();
    private final Map<Target, Integer> roundTargets = new HashMap<>();
    private final Set<Target> dontShootTargets = new HashSet<>();
    private final Set<Target> layoutTargets = new HashSet<>();

    private double widthScaleFactor = 1;
    private double heightScaleFactor = 1;
    private int score = 0;
    private int roundIndex = 0;

    public ShootOut() {
    }

    public ShootOut(List<Target> targets) {
        super(targets);
    }

    @Override
    public void init() {
        widthScaleFactor = getArenaWidth() / DEFAULT_WIDTH;
        heightScaleFactor = getArenaHeight() / DEFAULT_HEIGHT;

        initializeCourses();
        initializeGui();

        startRound(courses.get(roundIndex));
    }

    private double scaleX(double x) {
        return x * widthScaleFactor + (10 * widthScaleFactor);
    }

    private double scaleY(double y) {
        return y * heightScaleFactor + (10 * heightScaleFactor);
    }

    private double scaleWidth(double width) {
        return width * widthScaleFactor;
    }

    private double scaleHeight(double height) {
        return height * heightScaleFactor;
    }

    private class Cover extends BoundingBox {
        public Cover(double minX, double minY, double width, double height) {
            super(scaleX(minX), scaleY(minY), scaleWidth(width), scaleHeight(height));
        }
    }

    private class ClipArea extends BoundingBox {
        private final Optional<Cover> cover;
        private final PerceivedDistance distance;

        public ClipArea(PerceivedDistance distance, double minX, double minY, double width, double height) {
            super(scaleX(minX), scaleY(minY), scaleWidth(width), scaleHeight(height));

            this.distance = distance;
            this.cover = Optional.empty();
        }

        public ClipArea(PerceivedDistance distance, Cover cover, double minX, double minY, double width,
                double height) {
            super(scaleX(minX), scaleY(minY), scaleWidth(width), scaleHeight(height));

            this.distance = distance;
            this.cover = Optional.of(cover);
        }

        public PerceivedDistance getPerceivedDistance() {
            return distance;
        }

        public Optional<Cover> getCover() {
            return cover;
        }
    }

    private enum PerceivedDistance {
        NEAR, MEDIUM, FAR
    }

    private void initializeCourses() {
        final Cover bigWindowCover = new Cover(158.5, 168, 47, 27);
        final ClipArea bigWindow = new ClipArea(PerceivedDistance.MEDIUM, bigWindowCover, 158.5, 101, 37, 87);

        final ClipArea bottomLeftCorner = new ClipArea(PerceivedDistance.NEAR, 0, 415, 257, 65);

        final ClipArea bottomRightCorner = new ClipArea(PerceivedDistance.NEAR, 522, 397, 118, 83);

        final Cover carCover = new Cover(247, 353, 318, 50);
        final ClipArea car = new ClipArea(PerceivedDistance.NEAR, carCover, 247, 264, 318, 115);

        final Cover halfWindowCover = new Cover(275, 176, 57, 27);
        final ClipArea halfWindow = new ClipArea(PerceivedDistance.MEDIUM, halfWindowCover, 278, 135, 27, 49);

        final ClipArea sniperPosition = new ClipArea(PerceivedDistance.FAR, 409, 201, 31, 45);

        final List<ClipArea> oradourBoundingBox = new ArrayList<>();
        oradourBoundingBox.add(bigWindow);
        oradourBoundingBox.add(bottomLeftCorner);
        oradourBoundingBox.add(bottomRightCorner);
        oradourBoundingBox.add(car);
        oradourBoundingBox.add(halfWindow);
        oradourBoundingBox.add(sniperPosition);

        courses.add("/arena/backgrounds/oradour-sur-glane.gif");
        courseBoundingBoxes.put("/arena/backgrounds/oradour-sur-glane.gif", oradourBoundingBox);

        final ClipArea walkwayNear = new ClipArea(PerceivedDistance.NEAR, 331, 341, 469, 125);
        final ClipArea walkwayMedium = new ClipArea(PerceivedDistance.MEDIUM, 319, 216, 229, 66.5);
        final ClipArea walkwayFar = new ClipArea(PerceivedDistance.FAR, 289, 148, 116, 42);

        final List<ClipArea> parkingLotBoundingBox = new ArrayList<>();
        parkingLotBoundingBox.add(walkwayNear);
        parkingLotBoundingBox.add(walkwayMedium);
        parkingLotBoundingBox.add(walkwayFar);

        courses.add("/arena/backgrounds/subterranean_parking_lot.gif");
        courseBoundingBoxes.put("/arena/backgrounds/subterranean_parking_lot.gif", parkingLotBoundingBox);
    }

    // Paints targets over the ClipArea and cover bounds for the course
    // to aid in debugging the course layout
    @SuppressWarnings("unused")
    private void visualizeCourse(String coursePath) {
        for (ClipArea clipArea : courseBoundingBoxes.get(coursePath)) {
            final Optional<Target> clipTarget = addTarget(new File("targets/clip_area.target"), 0, 0);

            if (clipTarget.isPresent()) {
                final Target t = clipTarget.get();

                t.setPosition(clipArea.getMinX(), clipArea.getMinY());
                t.setDimensions(clipArea.getWidth(), clipArea.getHeight());

                layoutTargets.add(t);
            }

            Optional<Cover> cover = clipArea.getCover();
            if (cover.isPresent()) {
                final Optional<Target> coverTarget = addTarget(new File("targets/cover.target"), 0, 0);

                if (clipTarget.isPresent()) {
                    final Target t = coverTarget.get();
                    final BoundingBox b = cover.get();

                    t.setPosition(b.getMinX(), b.getMinY());
                    t.setDimensions(b.getWidth(), b.getHeight());

                    layoutTargets.add(t);
                }
            }
        }
    }

    private void initializeGui() {
        showTextOnFeed("Score: 0");

        final GridPane exercisePane = new GridPane();

        exercisePane.add(new Label("Maximum Visible Targets"), 0, 0);
        exercisePane.add(maxTargetsChoiceBox, 1, 0);

        for (int i = 1; i <= 10; i++) {
            maxTargetsChoiceBox.getItems().add(i);
        }

        maxTargetsChoiceBox.getSelectionModel().select(2);

        exercisePane.add(new Label("Required Hits Per Targets"), 0, 1);
        exercisePane.add(requiredHitsChoiceBox, 1, 1);

        for (int i = 1; i <= 5; i++) {
            requiredHitsChoiceBox.getItems().add(i);
        }

        requiredHitsChoiceBox.getSelectionModel().select(1);

        exercisePane.add(new Label("Inject Don't Shoot Targets"), 0, 2);
        exercisePane.add(injectDontShootCheckBox, 1, 2);

        addExercisePane(exercisePane);
    }

    private void startRound(String coursePath) {
        for (Target t : layoutTargets) {
            removeTarget(t);
        }

        layoutTargets.clear();

        for (Target t : roundTargets.keySet()) {
            removeTarget(t);
        }

        roundTargets.clear();

        for (Target t : dontShootTargets) {
            removeTarget(t);
        }

        dontShootTargets.clear();

        setArenaBackground(coursePath);
        // Uncomment this if debugging a new course
        // visualizeCourse(coursePath);

        final List<ClipArea> clipAreas = courseBoundingBoxes.get(coursePath);

        final Random rand = new Random();
        final int targetCount = rand.nextInt((maxTargetsChoiceBox.getValue() - 1) + 1) + 1;

        for (int i = 0; i < targetCount; i++) {
            // Ensure there is always at least one shoot target
            final boolean isDontShoot;
            if (injectDontShootCheckBox.isSelected() && dontShootTargets.size() < targetCount - 1) {
                isDontShoot = rand.nextBoolean();
            } else {
                isDontShoot = false;
            }

            // Pick a target at random
            final Optional<Target> target = addTarget(acceptedTargets.get(rand.nextInt(acceptedTargets.size())), 0, 0);

            if (!target.isPresent())
                continue;

            final Target t = target.get();

            // Pick a clip area at random
            final ClipArea clipArea = clipAreas.get(rand.nextInt(clipAreas.size()));

            // Resize appropriately for perceived distance
            final double scale;

            switch (clipArea.getPerceivedDistance()) {
            case NEAR:
                scale = (rand.nextInt((100 - 60) + 1) + 80) / 100d;
                break;

            case MEDIUM:
                scale = (rand.nextInt((70 - 50) + 1) + 50) / 100d;
                break;

            case FAR:
                scale = (rand.nextInt((40 - 20) + 1) + 20) / 100d;
                break;

            default:
                scale = 1;
            }

            final Dimension2D size = t.getDimension();
            t.setDimensions(size.getWidth() * scale, size.getHeight() * scale);

            // Place the target in the clip area
            final int minX = (int) clipArea.getMinX();
            final int maxX;
            if (clipArea.getWidth() > size.getWidth()) {
                // For wide areas ensure at least half the target is visible
                maxX = (int) (clipArea.getMaxX() - (size.getWidth() / 2));
            } else {
                maxX = (int) (clipArea.getMaxX() - MIN_TARGET_VISIBLE);
            }

            final int minY = (int) clipArea.getMinY();
            final int maxY = (int) (clipArea.getMaxY() - MIN_TARGET_VISIBLE);

            final int x = rand.nextInt((maxX - minX) + 1) + minX;
            final int y;

            if (clipArea.getCover().isPresent()) {
                final BoundingBox cover = clipArea.getCover().get();

                final int potentialMinY = (int) (cover.getMinY() - t.getDimension().getHeight());
                if (clipArea.getMaxY() - potentialMinY < MIN_TARGET_VISIBLE) {
                    y = (int) (cover.getMinY() - (clipArea.getMaxY() - potentialMinY));
                } else {
                    y = (int) cover.getMinY();
                }
            } else {
                y = rand.nextInt((maxY - minY) + 1) + minY;
            }

            final int adjustedX = (int) (x - t.getBoundsInParent().getMinX() - (t.getDimension().getWidth() / 2));
            final int adjustedY = (int) (y - t.getBoundsInParent().getMinY() - (t.getDimension().getHeight() / 2));
            t.setPosition(adjustedX, adjustedY);

            // Clip the target if it has cover
            if (clipArea.getCover().isPresent()) {
                final Point2D convertedCoords = t.parentToLocal(clipArea.getMinX(), clipArea.getMinY());
                final Rectangle clip = new Rectangle(convertedCoords.getX(), convertedCoords.getY(),
                        clipArea.getWidth(), clipArea.getHeight());

                // The clip area has the target's scale applied to it, thus we
                // need
                // to set a scale for the clip to counteract this
                clip.setScaleX((clipArea.getWidth() / (clipArea.getWidth() * scale)));
                clip.setScaleY((clipArea.getHeight() / (clipArea.getHeight() * scale)));

                t.setClip(clip);
            }

            // Add targets to map to keep track of hit count
            if (isDontShoot) {
                for (TargetRegion r : t.getRegions()) {
                    r.setFill(Color.RED);
                }

                dontShootTargets.add(t);
            } else {
                roundTargets.put(t, 0);
            }
        }
    }

    @Override
    public void reset(List<Target> targets) {
        showTextOnFeed("Score: 0");
        score = 0;

        roundIndex = 0;
        startRound(courses.get(roundIndex));
    }

    @Override
    public void shotListener(Shot shot, Optional<Hit> hit) {
        boolean badShoot = false;

        if (hit.isPresent()) {
            final Target hitTarget = hit.get().getTarget();

            if (roundTargets.containsKey(hitTarget)) {
                roundTargets.put(hitTarget, roundTargets.get(hitTarget) + 1);

                if (roundTargets.get(hitTarget) >= requiredHitsChoiceBox.getValue()) {
                    roundTargets.remove(hitTarget);
                    removeTarget(hitTarget);
                }

                final TargetRegion hitRegion = hit.get().getHitRegion();

                if (hitRegion.tagExists("points")) {
                    score += Integer.parseInt(hitRegion.getTag("points"));
                }
            } else if (dontShootTargets.contains(hitTarget)) {
                badShoot = true;

                TextToSpeech.say(String.format("Your score was %d", score));
                score = 0;
            }
        }

        showTextOnFeed(String.format("Score: %d", score));

        if (roundTargets.isEmpty() || badShoot) {
            roundIndex++;

            if (roundIndex >= courses.size()) {
                roundIndex = 0;
            }

            startRound(courses.get(roundIndex));
        }
    }

    @Override
    public void targetUpdate(Target target, TargetChange change) {
    }

    @Override
    public ExerciseMetadata getInfo() {
        return new ExerciseMetadata("Shoot Out", "1.0", "phrack",
                "Scored targets are randomly displayed in scene appropriate locations while "
                        + "hiding behind cover. Get the minimum number of shots on each target to get a "
                        + "new scene or target arrangement. For an extra challenge, turn on don't shoot "
                        + "targets and avoid hitting the all red targets.");
    }
}
