package com.gmail.ed3v8vl0.HardcoreSurvival.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.configuration.file.FileConfiguration;

import com.gmail.ed3v8vl0.HardcoreSurvival.Data.SurvivalCalc.MoneyReward.Reward;

public class SurvivalCalc {
	//HardCore
	public static double damageReceive;
	public static double damageAttack;
	public static double hungrySpeed;
	public static float xpReceive;
	
	//Update
	public static long lastTime;
	public static double lastPercent;
	public static long lastLimit;
	
	//PlayerStat
	public static class Attack {
		public static double AddPlayerDamage;
		public static double AddCreatureDamage;
		public static double AddProjectileDamage;
	}
	
	public static class Resistance {
		public static double DamageReduction;
		public static double ProjectileDamageReduction;
		public static double AddHealth;
	}
	
	public static class Evasion {
		public static double AddSpeed;
		public static double SprintEvasionPercent;
		public static long SprintEvasionTime; //Config Add
	}
	
	public static class Lucky {
		public static int IFAttackCount;
		public static double AddDamagePercent;
		public static double AddDamageValue;
		public static double ArmorDamageReduction;
	}
	
	public static class MoneyReward {
		public static double Frist;
		public static double Second;
		public static double Third;
		public static double FirstBlood;
		public static double Kill;
		public static double KillingSpree;
		public static double Supply;
		public static double ConsecutiveWin;
		public static List<Reward> KillRewards;
		
		public static class Reward implements Entry<Integer, Double> {
		    private Integer key;
		    private Double value;

		    public Reward(Integer key, Double value) {
		        this.key = key;
		        this.value = value;
		    }
		    
			@Override
			public Integer getKey() {
				// TODO Auto-generated method stub
				return key;
			}

			@Override
			public Double getValue() {
				// TODO Auto-generated method stub
				return value;
			}

			public Integer setKey(Integer key) {
				Integer old = this.key;
		        this.key = key;
				return old;
			}
			
			@Override
			public Double setValue(Double value) {
				Double old = this.value;
		        this.value = value;
				return old;
			}
		}
	}
	
	public static void Load(FileConfiguration config) {
		SurvivalCalc.lastTime = config.getLong("worldborder.lastTime");
		SurvivalCalc.lastPercent = config.getDouble("worldborder.lastPercent");
		SurvivalCalc.lastLimit = config.getLong("worldborder.lastLimit");
		
		SurvivalCalc.damageReceive = config.getDouble("Calc.damageReceive");
		SurvivalCalc.damageAttack = config.getDouble("Calc.damageAttack");
		SurvivalCalc.hungrySpeed = config.getDouble("Calc.hungrySpeed");
		SurvivalCalc.xpReceive = (float) config.getDouble("Calc.xpReceive");
		
		SurvivalCalc.Attack.AddPlayerDamage = config.getDouble("Calc.Attack.AddPlayerDamage");
		SurvivalCalc.Attack.AddCreatureDamage = config.getDouble("Calc.Attack.AddCreatureDamage");
		SurvivalCalc.Attack.AddProjectileDamage = config.getDouble("Calc.Attack.AddProjectileDamage");
		
		SurvivalCalc.Resistance.DamageReduction = config.getDouble("Calc.Resistance.DamageReduction");
		SurvivalCalc.Resistance.ProjectileDamageReduction = config.getDouble("Calc.Resistance.ProjectileDamageReduction");
		SurvivalCalc.Resistance.AddHealth = config.getDouble("Calc.Resistance.AddHealth");
		
		SurvivalCalc.Evasion.AddSpeed = config.getDouble("Calc.Evasion.AddSpeed");
		SurvivalCalc.Evasion.SprintEvasionTime = config.getLong("Calc.Evasion.SprintEvasionTime");
		SurvivalCalc.Evasion.SprintEvasionPercent = config.getDouble("Calc.Evasion.SprintEvasionPercent");

		SurvivalCalc.Lucky.IFAttackCount = config.getInt("Calc.Lucky.IFAttackCount");
		SurvivalCalc.Lucky.AddDamagePercent = config.getDouble("Calc.Lucky.AddDamagePercent");
		SurvivalCalc.Lucky.AddDamageValue = config.getDouble("Calc.Lucky.AddDamageValue");
		SurvivalCalc.Lucky.ArmorDamageReduction = config.getDouble("Calc.Lucky.ArmorDamageReduction");
		

		SurvivalCalc.MoneyReward.Frist = config.getDouble("Calc.Money.First");
		SurvivalCalc.MoneyReward.Second = config.getDouble("Calc.Money.Second");
		SurvivalCalc.MoneyReward.Third = config.getDouble("Calc.Money.Third");
		SurvivalCalc.MoneyReward.FirstBlood = config.getDouble("Calc.Money.FirstBlood");
		SurvivalCalc.MoneyReward.Kill = config.getDouble("Calc.Money.Kill");
		SurvivalCalc.MoneyReward.KillingSpree = config.getDouble("Calc.Money.KillingSpree");
		SurvivalCalc.MoneyReward.Supply = config.getDouble("Calc.Money.Supply");
		SurvivalCalc.MoneyReward.ConsecutiveWin = config.getDouble("Calc.Money.ConsecutiveWin");;
		
		SurvivalCalc.MoneyReward.KillRewards = new ArrayList<Reward>();
		
		try {
			for (String s : config.getStringList("Calc.Money.KillRewards")) {
				String[] split = s.split(":");

				SurvivalCalc.MoneyReward.KillRewards.add(new Reward(Integer.parseInt(split[0]), Double.parseDouble(split[1])));
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}
}
