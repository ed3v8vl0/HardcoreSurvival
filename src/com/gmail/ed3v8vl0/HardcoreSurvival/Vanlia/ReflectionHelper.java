package com.gmail.ed3v8vl0.HardcoreSurvival.Vanlia;

import java.lang.reflect.Field;

import net.minecraft.server.v1_12_R1.EntityHuman;

public class ReflectionHelper {
	public static void ReflectionFood(EntityHuman entityHuman, FoodMetaDataEx foodData) {
		try {
			Field field = EntityHuman.class.getDeclaredField("foodData");
			
			field.setAccessible(true);
			field.set(entityHuman, foodData);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}