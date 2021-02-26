package com.nopalsoft.donttap.game;

import java.util.Iterator;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Array.ArrayIterator;
import com.badlogic.gdx.utils.Pools;
import com.nopalsoft.donttap.objetos.Tiles;
import com.nopalsoft.donttap.screens.Screens;

public class WorldGame extends Table {
	static public final int STATE_READY = 0;
	static public final int STATE_RUNNING = 1;
	static public final int STATE_GAME_OVER_1 = 2;
	static public final int STATE_GAME_OVER_2 = 3;
	static public final int STATE_GAME_WIN = 4;
	public int state;

	public float TIME_TO_SPWAN_ROW = .4f;
	float timeToSpwanRow = 0;

	static public final int MODE_CLASSIC = 0;// Reccore 50 tiles en el menor tiempo
	static public final int MODE_TIME = 1;// Recorre por 1 minuto
	static public final int MODE_ENDLESS = 2;// No dejes que ningun tile se pase
	public int mode;

	final float WIDTH = Screens.WORLD_WIDTH;
	final float HEIGHT = Screens.WORLD_HEIGHT;

	Array<Tiles> arrTiles;

	public float time;
	public int score;

	public WorldGame(int mode) {
		this.mode = mode;
		setSize(WIDTH, HEIGHT);
		setPosition(0, 0);
		debug();

		arrTiles = new Array<Tiles>();
		addRow(true);
		addRow();
		addRow();
		addRow();

		state = STATE_READY;

		switch (mode) {
		case MODE_CLASSIC:
			score = 100;
			time = 0;
			break;
		case MODE_TIME:
			score = 0;
			time = 60;
			break;

		case MODE_ENDLESS:
			score = 0;
			time = 0;
			timeToSpwanRow = TIME_TO_SPWAN_ROW;
			break;
		}
	}

	public void addRow() {
		if (state == STATE_GAME_OVER_1 || state == STATE_GAME_OVER_2)
			return;
		addRow(false);
	}

	public void addRow(boolean isFirstRow) {

		int tileCanStep = MathUtils.random(3);

		// Agrego toda una fila en Y= -1
		// para despues bajar todos los tiles 1 renglon
		for (int col = 0; col < 4; col++) {
			Tiles obj = Pools.obtain(Tiles.class);

			if (tileCanStep == col)
				obj.init(this, col, true, isFirstRow);
			else
				obj.init(this, col, false, isFirstRow);

			addActor(obj);
			arrTiles.add(obj);
		}

		moveRowsDown();

	}

	public void moveRowsDown() {
		for (int col = 23; col >= 0; col--) {
			Tiles obj = getPiezaEnPos(col);
			if (obj != null)
				obj.moveDown();
		}
	}

	public void moveRowsUp() {
		for (int col = 0; col < 24; col++) {
			Tiles obj = getPiezaEnPos(col);
			if (obj != null)
				obj.moveUp();
		}
	}

	@Override
	public void act(float delta) {
		super.act(delta);

		if (state == STATE_GAME_OVER_1) {
			int moveActions = 0;
			Iterator<Tiles> i = arrTiles.iterator();
			while (i.hasNext()) {
				Iterator<Action> itAc = i.next().getActions().iterator();
				while (itAc.hasNext()) {
					if (itAc.next() instanceof MoveToAction)
						moveActions++;
				}
			}
			if (moveActions == 0)
				state = STATE_GAME_OVER_2;
			return;
		}

		if (state == STATE_RUNNING) {
			if (mode == MODE_TIME) {
				time -= delta;
				if (time <= 0)
					time = 0;
			}
			else {
				time += delta;
			}
			if (mode == MODE_ENDLESS) {
				timeToSpwanRow += delta;
				if (timeToSpwanRow >= TIME_TO_SPWAN_ROW) {
					timeToSpwanRow -= TIME_TO_SPWAN_ROW;

					TIME_TO_SPWAN_ROW -= (delta / 8f);
					if (TIME_TO_SPWAN_ROW <= .2f)
						TIME_TO_SPWAN_ROW = .2f;
					addRow();
				}

			}

		}

		deleteOld();
		updatesTiles();
		if (checkIsGameover())
			state = STATE_GAME_OVER_1;

		if (checkGameWin())
			state = STATE_GAME_WIN;

	}

	// Actualizo los tiles que ya se pueden tocar
	private void updatesTiles() {
		for (int col = 0; col < 24; col++) {
			Tiles obj = getPiezaEnPos(col);
			if (obj != null) {
				if (obj.state == Tiles.STATE_TAP) {
					Tiles previosObj = findPreviosTapTile(col - 1);
					if (previosObj != null)
						previosObj.canBeTap = true;

				}
			}

		}

	}

	private Tiles findPreviosTapTile(int posActual) {
		for (int col = posActual; col >= 0; col--) {
			Tiles obj = getPiezaEnPos(col);
			if (obj != null) {
				if (obj.tipo == Tiles.TIPO_BUENO)
					return obj;
			}
		}
		return null;
	}

	private void deleteOld() {
		Iterator<Tiles> ite = arrTiles.iterator();
		while (ite.hasNext()) {
			Tiles obj = ite.next();
			if (obj.posicionTabla < 0 || obj.posicionTabla > 23) {
				removeActor(obj);
				arrTiles.removeValue(obj, true);
				Pools.free(obj);
			}

		}

	}

	private boolean checkIsGameover() {
		Iterator<Tiles> ite = arrTiles.iterator();
		while (ite.hasNext()) {
			Tiles obj = ite.next();
			if (obj.tipo == Tiles.TIPO_MALO && obj.state == Tiles.STATE_TAP)
				return true;

			if (obj.posicionTabla > 19 && obj.getY() < -90
					&& obj.tipo == Tiles.TIPO_BUENO
					&& obj.state == Tiles.STATE_NORMAL) {
				moveRowsUp();
				return true;
			}
		}
		if (mode == MODE_TIME)
			if (time <= 0)
				return true;
		return false;

	}

	private boolean checkGameWin() {
		switch (mode) {
		case MODE_CLASSIC:
			if (score == 0)
				return true;
			break;
		case MODE_TIME:
			if (time == 0)
				return true;
			break;

		default:
		case MODE_ENDLESS:
			return false;
		}
		return false;

	}

	private Tiles getPiezaEnPos(int posicionTabla) {
		ArrayIterator<Tiles> ite = new ArrayIterator<Tiles>(arrTiles);
		while (ite.hasNext()) {
			Tiles obj = ite.next();
			if (obj.posicionTabla == posicionTabla)
				return obj;
		}
		return null;
	}

}
