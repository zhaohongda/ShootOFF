package com.shootoff.plugins;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.shootoff.camera.Shot;
import com.shootoff.gui.targets.TargetView;
import com.shootoff.targets.Hit;
import com.shootoff.targets.Target;

import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

public class ScenarioVideos extends ProjectorTrainingExerciseBase implements TrainingExercise {
	private final WebView webview = new WebView();
	private final CheckBox pauseOnShotCheckBox = new CheckBox("Pause On Shot");
	
	// First param = YouTube video id
	private final String embeddedYouTubeHtml = "<!DOCTYPE html>\n" +
			"<html>\n" +
			"  <body>\n" +
			"    <div id='player'></div>\n" +
			
			"    <script>\n" +
			"      var tag = document.createElement('script');\n" +
			
			"      tag.src = 'https://www.youtube.com/iframe_api';\n" +
			"      var firstScriptTag = document.getElementsByTagName('script')[0];\n" +
			"      firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);\n" +
			
			"      var player;\n" +
			"      function onYouTubeIframeAPIReady() {\n" +
			"        player = new YT.Player('player', {\n" +
			"          height: '390',\n" +
			"          width: '640',\n" +
			"          videoId: '%s',\n" +
			"          events: {\n" +
			"            'onReady': onPlayerReady\n" +
			"          }\n" +
			"        });\n" +
			"      }\n" +
			
			"      function onPlayerReady(event) {\n" +
			"        event.target.playVideo();\n" +
			"      }\n" +
			
			"    </script>\n" +
			"  </body>\n" +
			"</html>";
	
	public ScenarioVideos() {}

	public ScenarioVideos(List<Target> targets) {
		super(targets);
	}

	@Override
	public void init() {
		initGui();
		
		final File videoTargetFile = new File("targets/video.target");
		final Optional<Target> videoTarget = addTarget(videoTargetFile, 0, 0);
		
		if (videoTarget.isPresent()) {
			webview.setPrefSize(660, 410);
			((TargetView) videoTarget.get()).addTargetChild(webview);
		}
		
		// TODO: Make video selectable
		webview.getEngine().loadContent(String.format(embeddedYouTubeHtml, "IGqz5_AtInQ"));
	}
	
	public void initGui() {
		final VBox exercisePane = new VBox(pauseOnShotCheckBox);
		addExercisePane(exercisePane);
	}

	@Override
	public void reset(List<Target> targets) {
		webview.getEngine().executeScript("player.seekTo(0, true); player.playVideo();");
	}

	@Override
	public void shotListener(Shot shot, Optional<Hit> hit) {
		if (pauseOnShotCheckBox.isSelected()) {
			if (Platform.isFxApplicationThread()) {
				webview.getEngine().executeScript("player.pauseVideo();");
			} else {
				Platform.runLater(() -> webview.getEngine().executeScript("player.pauseVideo();"));
			}
		}
	}

	@Override
	public void targetUpdate(Target target, TargetChange change) {}
	
	@Override
	public ExerciseMetadata getInfo() {
		return new ExerciseMetadata("Scenario Videos", "1.0", "phrack",
				"This exercise pairs videos with hit box scripts to put you in real-world "
				+ "scenarios with precise hit tracking.");
	}
}
