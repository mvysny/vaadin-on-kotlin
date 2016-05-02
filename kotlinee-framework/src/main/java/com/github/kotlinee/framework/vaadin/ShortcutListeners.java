package com.github.kotlinee.framework.vaadin;

import org.jetbrains.annotations.NotNull;

import com.vaadin.event.ShortcutListener;

/**
 * @author mvy
 */
public class ShortcutListeners {
	public static ShortcutListener listener(int keyCode, @NotNull int[] modifierKeys, final @NotNull Runnable action) {
		return new ShortcutListener(null, keyCode, modifierKeys) {
			@Override
			public void handleAction(Object sender, Object target) {
				action.run();
			}
		};
	}
}
