/*
 * ShootOFF - Software for Laser Dry Fire Training
 * Copyright (C) 2015 phrack
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

import java.util.Set;

import javax.sound.sampled.AudioInputStream;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.AudioPlayer;

public final class TextToSpeech {
	private static boolean inited = false;
	private static boolean isSilenced = false;
	private static MaryInterface marytts = null;
	
	protected TextToSpeech() {}
	
	private static void init() throws MaryConfigurationException {
		marytts = new LocalMaryInterface();
		Set<String> voices = marytts.getAvailableVoices();
		marytts.setVoice(voices.iterator().next());
		inited = true;
	}
	
	public static void say(String comment) {		
		if (isSilenced) {
			System.out.println(comment);
			return;
		}
		
		try {
			if(!inited) {
				init();
			}
		
			if (comment.isEmpty()) return;
			
			AudioInputStream audio = marytts.generateAudio(comment);
			AudioPlayer player = new AudioPlayer(audio);
			player.start();
		} catch (MaryConfigurationException | 
				SynthesisException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Allows TTS to be silenced or on. If silenced, instead of 
	 * saying a comment the desired comment will be printed to
	 * stdout. This exists so that components can be easily tested
	 * even if they are reliant on TTS. 
	 * 
	 * @param isSilenced set to <tt>true</tt> if spoken phrases
	 * 					 should instead be printed to stdout,
	 * 					 <tt>false</tt> for normal operation.
	 */
	public static void silence(boolean isSilenced) {
		TextToSpeech.isSilenced = isSilenced;
	}
}