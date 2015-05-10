/*
 * Copyright (c) 2015 phrack. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package com.shootoff.camera;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;

public class Shot {
	private final Color color;
	private final double x;
	private final double y;
	private final int timestamp;
	private final Ellipse marker;
	
	public Shot (Color color, double x, double y, int timestamp, int markerRadius) {
		this.color = color;
		this.x = x;
		this.y = y;
		this.timestamp = timestamp;
		this.marker = new Ellipse(x, y, markerRadius, markerRadius);
		this.marker.setFill(color);
	}
	
	public void drawShot(Group canvasGroup) {
		Platform.runLater(() -> {
				canvasGroup.getChildren().add(marker);
			});
	}

	public Color getColor() {
		return color;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public int getTimestamp() {
		return timestamp;
	}
	
	public Ellipse getMarker() {
		return marker;
	}
}