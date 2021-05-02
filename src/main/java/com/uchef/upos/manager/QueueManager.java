package com.uchef.upos.manager;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class QueueManager {
	private static Map<String, Object> instances = new HashMap<String, Object>();

	private QueueManager() {}

	@SuppressWarnings("unchecked")
	public static <T> T Instance(String name) {
		Class<T> c;
		try {
			c = (Class<T>) Class.forName(name);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			return null;
		}

		if (!instances.containsKey(name)) {
			T inst;
			try {
				inst = c.getConstructor().newInstance();
			} catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException
					| InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
			instances.put(name, inst);
		}
		return c.cast(instances.get(name));
	}
}
