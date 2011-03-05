import java.awt.*;
import java.awt.event.*;
import java.applet.*;

public class BreakOut extends Applet implements Runnable,KeyListener,FocusListener {
  static final int PADDLE_WIDTH=50;
  static final int PADDLE_HEIGHT=4;
  static final int PADDLE_STEP=5;

  static final int BALL_DIAMETER=10;
  static final int BALL_STEP=2;

  // second screen for the double buffering technique:
  Image offscreenImage;
  Graphics offscreenGraphics;

  int width,height;

  BrickWall brickWall;
  Paddle paddle;
  Ball ball;

  boolean running,suspended,gameOver,waitingForSpace;

  volatile boolean rightPressed,leftPressed,spaceWasPressed;

  // ----------------------------------------------------------
  // Wait for space to be pressed

  void waitForSpace() {
    waitingForSpace=true;
    repaint();
    // when we sleep we consume less resources
    spaceWasPressed=false;
    while (!spaceWasPressed) {
      try {
        Thread.currentThread().sleep(100);
      }
      catch (InterruptedException e) {};
    }
    waitingForSpace=false;
  }

  // ----------------------------------------------------------
  // FocusListener

  // If the user clicks our applet, we start or resume the game
  public void focusGained(FocusEvent e) {
    if (!running) {
      suspended=false;
      running=true;
      (new Thread(BreakOut.this)).start();
    } else {
      suspended=false;
    }
    repaint();
  }

  // If the user clicks somewhere else, we suspend the game
  public void focusLost(FocusEvent e) {
    if (running) {
      suspended=true;
      repaint();
    }
  }

  // ----------------------------------------------------------
  // KeyListener

  public void keyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_LEFT:  leftPressed=true; break;
      case KeyEvent.VK_RIGHT: rightPressed=true; break;
      case KeyEvent.VK_SPACE: spaceWasPressed=true; break;
    }
  }

  public void keyReleased(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_LEFT:  leftPressed=false; break;
      case KeyEvent.VK_RIGHT: rightPressed=false; break;
    }
  }

  public void keyTyped(KeyEvent e) {  }

  // -----------------------------------------------------------

  public void init() {
    setBackground(Color.black);

    running=false;
    width=getSize().width;
    height=getSize().height;

    offscreenImage = createImage(width,height);
    offscreenGraphics = offscreenImage.getGraphics();

    addKeyListener(this);
    addFocusListener(this);

    waitingForSpace=false;

    repaint();
  }

  public void paint(Graphics g) {
    if (running) {
      g.drawImage(offscreenImage, 0, 0, this);
      if (suspended) {
        g.setColor(Color.white);
        g.drawString("Click here.",(width-70)/2,height/2);
      } else if (waitingForSpace){
        g.setColor(Color.white);
        g.drawString("Press SPACE.",(width-70)/2,height/2);
      }

    } else {
        g.setColor(Color.white);
        g.drawString("Click here to start.",(width-90)/2,height/2);
    }
  }

  public void update(Graphics g) {
    paint(g);
  }

  public void run() {
    while (true) {
      offscreenGraphics.setColor(Color.black);
      offscreenGraphics.fillRect(0,0,width,height);

      gameOver=false;

      brickWall=new BrickWall(10,4,width/10,height/(3*4),offscreenGraphics);
      brickWall.paint();
      paddle=new Paddle(PADDLE_WIDTH,PADDLE_HEIGHT,(width-PADDLE_WIDTH)/2,height-1-PADDLE_HEIGHT,0,width,PADDLE_STEP,offscreenGraphics);
      paddle.paint();
      ball=new Ball(width/2,height/3,0,BALL_STEP,BALL_DIAMETER,BALL_STEP,offscreenGraphics,0,width,0,height);
      ball.paint();

      repaint();

      waitForSpace();

      while (!gameOver) {
        try {
          Thread.currentThread().sleep(10);
        }
        catch (InterruptedException e) {};

        if (!suspended) {
          paddle.clear();
          ball.clear();

          if ((leftPressed)&&(!rightPressed)) paddle.moveLeft();
          if ((rightPressed)&&(!leftPressed)) paddle.moveRight();

          gameOver=ball.move(paddle,brickWall);
          if (brickWall.bricksLeft()==0) gameOver=true;

          paddle.paint();
          ball.paint();

          repaint();
        }
      }

      offscreenGraphics.setColor(Color.white);

      if (brickWall.bricksLeft()==0)
        offscreenGraphics.drawString("CONGRATULATIONS!",(width-120)/2,height/2-20);
      else
        offscreenGraphics.drawString("GAME OVER!",(width-76)/2,height/2-20);

      waitForSpace();
    }
  }
}
/*-------------------------------------------------------------------------------------------------
   class BrickWall - manages the bricks in the breakout game

   methods:
     void paint() - paints all the bricks
     int inBrick(int x,int y) - returns 1 if the point x,y is inside some brick
     void hitBrick(int x,int y) - deletes the brick which contains the point (x,y)
     int bricksLeft() - returns the number of bricks left

   exercise:
     modify the class, so that the bricks in even rows are shifted by 1/2 of the width of the brick
-------------------------------------------------------------------------------------------------*/

class BrickWall {
  private boolean brickVisible[][];
  private int bricksInRow,bricksInColumn,brickWidth,brickHeight,bricksLeft;
  Graphics g;

  public BrickWall(int bricksInRow_,int bricksInColumn_,int brickWidth_,int brickHeight_,Graphics g_) {
    bricksInRow=bricksInRow_;
    bricksInColumn=bricksInColumn_;
    brickWidth=brickWidth_;
    brickHeight=brickHeight_;
    g=g_;

    brickVisible=new boolean[bricksInRow][bricksInColumn];
    bricksLeft=0;

    int x,y;
    for (x=0;x<bricksInRow;x++)
      for (y=0;y<bricksInColumn;y++) {
        brickVisible[x][y]=true;
        bricksLeft++;
      }
  }

  public void paint() {
    int x,y;

    for (x=0;x<bricksInRow;x++)
      for (y=0;y<bricksInColumn;y++)
        if (brickVisible[x][y]) {
          g.setColor(Color.blue);
          g.fillRect(x*brickWidth,y*brickHeight,brickWidth-1,brickHeight-1);
        }
  }

  public int inBrick(int x,int y) {
    int nx,ny;

    nx=(x/brickWidth);
    ny=(y/brickHeight);

    if ((nx<0)||(nx>=bricksInRow)||(ny<0)||(ny>=bricksInColumn)) return 0;

    if (brickVisible[nx][ny]) return 1; else return 0;
  }

  public void hitBrick(int x,int y) {
    int nx,ny;

    nx=(x/brickWidth);
    ny=(y/brickHeight);

    if ((nx<0)||(nx>=bricksInRow)||(ny<0)||(ny>=bricksInColumn)) return;

    if (brickVisible[nx][ny]) {
      brickVisible[nx][ny]=false;
      bricksLeft--;
      g.setColor(Color.black);
      g.fillRect(nx*brickWidth,ny*brickHeight,brickWidth-1,brickHeight-1);
    }
  }

  public int bricksLeft() {
    return bricksLeft;
  }
}

/*-------------------------------------------------------------------------------------------------
   class Paddle - manages the paddle in the breakout game

   methods:
     void paint() - paints the paddle
     void clear() - clears the paddle
     void moveLeft() - moves the paddle to the left
     void moveRight() - moves the paddle to the right

   exercise:
     modify the class, so that the paddle can "go arround the screen"
-------------------------------------------------------------------------------------------------*/

class Paddle {
  private int width,height,x,y,maxx,minx,step;
  private Graphics g;

  public Paddle(int width_,int height_,int x_,int y_,int minx_,int maxx_,int step_,Graphics g_) {
    width=width_; height=height_; x=x_; y=y_; minx=minx_; maxx=maxx_; g=g_; step=step_;
  }

  public void paint() {
    g.setColor(Color.white);
    g.fillRect(x,y,width,height);
  }

  public void clear() {
    g.setColor(Color.black);
    g.fillRect(x,y,width,height);
  }

  public void moveLeft() {
    if (x-step>minx) x-=step; else x=minx;
  }

  public void moveRight() {
    if (x+step<maxx-width) x+=step; else x=maxx-width;
  }

  public int leftCorner() {
    return x;
  }

  public int rightCorner() {
    return x+width;
  }

  public int middle() {
    return x+width/2;
  }

  public int getY() {
    return y;
  }
}

/*-------------------------------------------------------------------------------------------------
   class Ball - manages the ball in the breakout game
   +
   methods:
     void paint() - paints the ball
     void clear() - clears the ball
     boolean move(Paddle paddle,BrickWall brickwall) - moves the ball, returns true
                                                       if the ball goes off the screen

-------------------------------------------------------------------------------------------------*/

class Ball {
  private int x,y,dx,dy,diameter,minx,maxx,miny,maxy,step;
  private Graphics g;

  public Ball(int x_,int y_,int dx_,int dy_,int diameter_,int step_,Graphics g_,int minx_,int maxx_,int miny_,int maxy_) {
    x=x_; y=y_; dx=dx_; dy=dy_; diameter=diameter_; step=step_; g=g_;
    minx=minx_; maxx=maxx_; miny=miny_; maxy=maxy_;
  }

  public void paint() {
    g.setColor(Color.white);
    g.fillOval(x,y,diameter,diameter);
  }

  public void clear() {
    g.setColor(Color.black);
    g.fillOval(x,y,diameter,diameter);
  }

  public boolean move(Paddle paddle,BrickWall brickWall) {
    boolean ballGoesOut=false;

    // If there is wall => bounce
    if ((x+dx<minx)||(x+dx+diameter>maxx)) dx=-dx;
    if (y+dy<0) dy=-dy;

    if (y+dy+diameter>=paddle.getY()) {
      if ((x+dx+diameter<paddle.leftCorner())||(x+dx>paddle.rightCorner()))
         ballGoesOut=true;
       else {
         dy=-dy;
         if (x+dx+diameter/2<paddle.middle())
           dx=-step;
         else
           dx=step;
       }
    }

    switch (brickWall.inBrick(x,y)+2*brickWall.inBrick(x+diameter,y)+4*brickWall.inBrick(x,y+diameter)+8*brickWall.inBrick(x+diameter,y+diameter)) {
      case 0: break;
      case 5: case 10: dx=-dx; break;
      case 3: case 12: dy=-dy; break;
      case 1: dx=step; dy=step; break;
      case 2: dx=-step; dy=step; break;
      case 4: dx=step; dy=-step; break;
      case 8: dx=-step; dy=-step; break;
      default: dx=-dx; dy=-dy; break;
    }

    brickWall.hitBrick(x,y);
    brickWall.hitBrick(x+diameter,y);
    brickWall.hitBrick(x,y+diameter);
    brickWall.hitBrick(x+diameter,y+diameter);

    x+=dx;
    y+=dy;

    return ballGoesOut;
  }
}
