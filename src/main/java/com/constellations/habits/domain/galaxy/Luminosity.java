package com.constellations.habits.domain.galaxy;

/**
 * Escala de brillo de una estrella compartida.
 *
 * <p>La estrella nunca desaparece por que alguien falle: se apaga o se enciende. Ese es
 * el punto del diseno. Con un "todos o nada", la primera persona que falla anula el
 * esfuerzo de las demas y el incentivo racional del resto pasa a ser no molestarse; con
 * una escala, tu cumplimiento siempre suma aunque el grupo se caiga.
 *
 * <p>El nivel {@value #MAX_LEVEL} se reserva al pleno. Podria repartirse la escala en
 * tramos iguales, pero "hoy cumplimos todos" es el unico dia que merece verse distinto
 * de un vistazo, y con tramos iguales un 95% se pintaria igual que un 100%.
 */
public final class Luminosity {

    /** Estrella apagada: nadie cumplio ese dia. */
    public static final int DARK = 0;

    /** Pleno del grupo. */
    public static final int MAX_LEVEL = 4;

    private Luminosity() {}

    public static int levelOf(int completions, int activeMembers) {
        if (activeMembers <= 0 || completions <= 0) {
            return DARK;
        }
        if (completions >= activeMembers) {
            return MAX_LEVEL;
        }

        double ratio = (double) completions / activeMembers;
        if (ratio <= 0.25) {
            return 1;
        }
        return ratio <= 0.5 ? 2 : 3;
    }
}
