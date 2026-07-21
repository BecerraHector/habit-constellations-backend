package com.constellations.habits.application.habit;

import com.constellations.habits.domain.habit.Habit;
import com.constellations.habits.domain.streak.HabitProgress;

/** Un habito junto con su estado calculado, que es lo que el cliente necesita pintar. */
public record HabitView(Habit habit, HabitProgress progress) {}
