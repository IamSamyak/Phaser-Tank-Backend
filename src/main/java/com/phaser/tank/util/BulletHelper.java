package com.phaser.tank.util;

public class BulletHelper {

    /**
     * Calculates the bullet's spawn tile based on tank's current tile (x, y) and direction (angle in degrees).
     *
     * @param x     Tank's tile x-position
     * @param y     Tank's tile y-position
     * @param angle Direction in degrees: 0 (up), 90 (right), 180 (down), 270 (left)
     * @return      int[]{tileX, tileY} where bullet should spawn
     */
    public static int[] getBulletActualPosition(int x, int y, int angle) {
        switch (angle) {
            case 0:   // Facing Up
                y -= 1;
                break;
            case 90:  // Facing Right
                x += 1;
                break;
            case 180: // Facing Down
                y += 1;
                break;
            case 270: // Facing Left
                x -= 1;
                break;
        }
        return new int[]{x, y};
    }
}
