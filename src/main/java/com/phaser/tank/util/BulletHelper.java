package com.phaser.tank.util;

import static com.phaser.tank.util.GameConstants.TILE_SIZE;

public class BulletHelper {

    public static int[] getBulletActualPosition(int x, int y, int angle) {
        switch (angle) {
            case 0:   // Facing Up
                y -= TILE_SIZE;
                break;
            case 90:  // Facing Right
                x += TILE_SIZE;
                break;
            case 180: // Facing Down
                y += TILE_SIZE;
                break;
            case 270: // Facing Left
                x -= TILE_SIZE;
                break;
        }
        return new int[]{x, y};
    }
}
