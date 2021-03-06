package states;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.Timer;

import entity.Animation;
import entity.Enemy;
import entity.Player;
import items.Bonus;
import items.Boom;
import items.HUD;
import main.GamePanel;
import tilemaps.TileMap;
import util.Progress;
import util.Sprite;

public class LevelState extends GameState implements ActionListener {
	
	Timer timer = new Timer(2500, this);
	public ArrayList<Enemy> enemies;
	public ArrayList<Enemy> enemyQueue;
	private ArrayList<Boom> booms;

	private Animation spawnAnimation;
	private Sprite spawnSprite;
	private boolean toSpawn;
	private Rectangle backButton;

	private boolean enemyFreeze;
	private long timeFreeze;
	private long finishTime;

	private int id;
	private int levelID;
	private int score;

	private Player player;
	public TileMap tileMap;
	private HUD hud;
	private Bonus bonus;
	private Progress pr;
	
	public LevelState(GameStateManager gsm) {
		this.gsm = gsm;
		init();
	}
	
	public void init() {
		enemies = new ArrayList<>();
		pr = Progress.getInstance();
		levelID = Integer.parseInt(pr.get("levelToPlay"));
		tileMap = new TileMap("/Levels/Level_"+levelID, 16);
		tileMap.loadTiles("/Images/tileset2.png");

		spawnSprite = new Sprite("/Images/image.png");
		spawnSprite.loadImages(16, 6, 4, 1, 16, 0, 0);
		spawnAnimation = new Animation(spawnSprite.getSpriteArray(0), -1);
		booms = new ArrayList<>();

		player = new Player(tileMap);
		timer.start();

		enemyQueue = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			enemyQueue.add(new Enemy(Integer.parseInt(tileMap.getEnemyTypes()[i]),
					(i % 3)+1, tileMap, player, i == 3 || i == 10 || i == 17));
			/*enemyQueue.add(new Enemy(Integer.parseInt(tileMap.getEnemyTypes()[i]),
					(i % 3)+1, tileMap, player, true));*/
		}
		id = 0;
		score = 0;
		finishTime = 0;
		hud = new HUD(enemies, player, levelID);
		backButton = new Rectangle(90, 720, 70, 70);

		for (int i = 0; i < 4; i++) {
			pr.set("killed"+(i+1), 0+"");
		}
		pr.set("currentScore", 0+"");
		pr.store();
	}

	@Override
	public void update() {
		tileMap.update();
		player.update();
		for(int i = 0; i < player.bullets.size(); i++) {
			boolean remove = player.bullets.get(i).update();
			if(remove) {
				booms.add(new Boom(player.bullets.get(i).getX()-45, player.bullets.get(i).getY()-45, false));
				player.bullets.remove(i);
				i--;
			}
		}
		for (int i = 0; i < enemies.size(); i++) {
			enemies.get(i).update();
		}
		for(int i = 0; i < enemies.size(); i++) {
			if(player.getRect().intersects(enemies.get(i).getRect())) {
				enemies.get(i).stop = true;
				player.stop = true;
			}
		}
		player.checkCollisions();
		for (int i = 0; i < enemies.size(); i++) {
			enemies.get(i).checkCollisions();
		}

		for (int i = 0; i < enemies.size(); i++) {
			for (int j = 0; j < enemies.size(); j++) {
				if(!enemies.get(i).equals(enemies.get(j))) {
					if(enemies.get(i).getRect().intersects(enemies.get(j).getRect())) {
						enemies.get(j).stop = true;
						enemies.get(j).checkCollisions();
						enemies.get(i).stop = true;
						enemies.get(i).checkCollisions();
					}
				}
			}
		}

		for(int i = 0; i < enemies.size(); i++) {
			for(int j = 0; j < enemies.get(i).enemyBullets.size(); j++) {
				boolean remove = enemies.get(i).enemyBullets.get(j).update();
				if(remove) {
					booms.add(new Boom(enemies.get(i).enemyBullets.get(j).getX()-45, enemies.get(i).enemyBullets.get(j).getY()-45, false));
					enemies.get(i).enemyBullets.remove(j);
					j--;
				}
				if(enemies.get(i).enemyBullets.get(j).getRectUp().intersects(player.getRect())) {
					if(!player.isRespawn()) {
						player.respawn(5000);
						player.setX(593);
						player.setY(766);
						player.level = 1;
						player.lives--;
					}
				}
			}
		}

		for(int i = 0; i < enemies.size(); i++) {
			for(int j = 0; j < player.bullets.size(); j++) {
				if(player.bullets.get(j).getRectUp().intersects(enemies.get(i).getRect())) {
					enemies.get(i).lives--;
					if(enemies.get(i).dead) {
						if(enemies.get(i).isBonus()) {
							if(bonus != null) bonus.create();
							else bonus = new Bonus(player, tileMap);
						}
						booms.add(new Boom(enemies.get(i).getX()-30, enemies.get(i).getY()-30, true));
						pr.set("killed"+enemies.get(i).level, (Integer.parseInt(pr.get("killed"+enemies.get(i).level))+1)+"");
						score += 100*enemies.get(i).level;
						enemies.remove(i);
						if(id < 20) toSpawn = true;
					}
					player.bullets.get(j).setRemove(true);
				}
			}
		}
		for(int i = 0; i < enemies.size(); i++) {
			for(int j = 0; j < enemies.get(i).enemyBullets.size(); j++) {
				for(int g = 0; g < player.bullets.size(); g++) {
					if(player.bullets.get(g).getRectUp().intersects(enemies.get(i).enemyBullets.get(j).getRectUp())) {
						player.bullets.remove(g);
						enemies.get(i).enemyBullets.remove(j);
					}
				}
			}
		}

		for (int i = 0; i < booms.size(); i++) {
			boolean remove = booms.get(i).update();
			if(remove) booms.remove(i);
		}
		if(bonus != null)
			if(player.getRect().intersects(bonus.getRect()) && bonus.isVisible()) {
				score += 500;
				bonus.setCatched(true);
				if(bonus.getType() == 1) player.respawn(10000);
				else if(bonus.getType() == 2) {
					for (Enemy enemy:enemies) {
						enemy.setFreeze();
						enemyFreeze = true;
						timeFreeze = System.currentTimeMillis();
					}
				}
				else if(bonus.getType() == 3) tileMap.armor();
				else if(bonus.getType() == 4) player.upgrade();
				else if(bonus.getType() == 5) {
					for (int i = 0; i < enemies.size(); i++) {
						booms.add(new Boom(enemies.get(i).getX()-30, enemies.get(i).getY()-30, true));
						enemies.remove(i);
						i--;
						if(id < 20) toSpawn = true;
					}
				}
			}

		hud.update(id);
		spawnAnimation.update();
		spawnUpdate();
		if(finishTime != 0)
			if(System.currentTimeMillis() - finishTime > 5000) {
				pr.set("currentScore", score+"");
				pr.set("xp", (Integer.parseInt(pr.get("xp"))+score)+"");
				pr.store();
				gsm.setState(gsm.COUNTSTATE);
			}
		if(bonus != null) bonus.update();
		System.out.println(score);
	}
	
	@Override
	public void draw(Graphics2D g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);
		g.setColor(Color.decode("#636363"));
		g.fillRect(0, 0, 310, GamePanel.HEIGHT);
		g.fillRect(GamePanel.WIDTH - 310, 0, 310, GamePanel.HEIGHT);

		tileMap.draw(g);
		player.draw(g);

		for(int i = 0; i < player.bullets.size(); i++) {
			player.bullets.get(i).draw(g);
		}
		for(int i = 0; i < enemies.size(); i++) {
			enemies.get(i).draw(g);
		}
		for(int i = 0; i < enemies.size(); i++) {
			for(int j = 0; j < enemies.get(i).enemyBullets.size(); j++) {
				enemies.get(i).enemyBullets.get(j).draw(g);
			}
		}

		if(spawnAnimation.getImage() != null) g.drawImage(spawnAnimation.getImage(), enemyQueue.get(id).getX(), enemyQueue.get(id).getY(), 56, 56, null);

		for (int i = 0; i < booms.size(); i++) {
			booms.get(i).draw(g);
		}

		tileMap.drawGrass(g);
		hud.draw(g);
		if(bonus != null) bonus.draw(g);
	}

	public void spawnUpdate() {
		if(enemyFreeze && System.currentTimeMillis() - timeFreeze > 5000) enemyFreeze = false;
		if(toSpawn && !enemyFreeze) {
			if(spawnAnimation.getDelay() == -1) spawnAnimation.setDelay(200);
			if(spawnAnimation.hasPlayed(2)) {
				boolean canSpawn = true;
				for (int i = 0; i < enemies.size(); i++) {
					if(enemies.get(i).getRect().intersects(enemyQueue.get(id).getRect())) canSpawn = false;
				}
				if(player.getRect().intersects(enemyQueue.get(id).getRect())) canSpawn = false;
				if(canSpawn) {
					enemies.add(enemyQueue.get(id));
					spawnAnimation.setDelay(-1);
					id++;
					toSpawn = false;
				}
			}
		}
		if(id == 20 && enemies.isEmpty() && finishTime==0) {
			System.out.println("Player win");
			finishTime = System.currentTimeMillis();
		}
	}

	@Override
	public void keyPressed(int k) {
		player.keyPressed(k);
		if(k == KeyEvent.VK_ESCAPE) {
			gsm.setState(gsm.MENUSTATE);
		}
	}
	
	@Override
	public void keyReleased(int k) {
		player.keyReleased(k);
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(backButton.contains(e.getPoint())) gsm.setState(gsm.MENUSTATE);
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		
	}
	@Override
	public void mouseExited(MouseEvent e) {
		
	}
	@Override
	public void mousePressed(MouseEvent e) {
		
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		
	}
	@Override
	public void mouseDragged(MouseEvent e) {

	}
	@Override
	public void mouseMoved(MouseEvent e) {

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(enemies.size() < 4 && id < 20) {
			toSpawn = true;
		}
	}
	
}
