import java.applet.*;
import java.awt.*;
import java.math.*;
import java.lang.*;
import java.util.Random;

//THIS IS THE RELIABLE CODE WITH NO COLLISION DETECTION B/W BEADS AND WITHOUT
//THE CLASS IMPLEMENTATION OF BEADS. THE CLASS CODE IS HERE BUT NOT USED

/*		<applet code="Beads" width=1300 height=600>
		 </applet>
*/
public class Beads extends Applet implements Runnable {

    class Bead {
        public double x, y, vx, vy, ax, ay, m;
        public Bead next, prev;


        public Bead(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.ax = 0.0;
            this.ay = 0.0;
            this.m = 2.0;
            this.next = this.prev = this;
        }

        public boolean isHead() {
            return (this.next == null);
        }
    }

    //Initial parameters
    int n = 1500;
    double[] x = new double[n];
    double[] y = new double[n];
    double d_eq;
    //set dimensions of the table and the "lip" post at the end of it
    int tableLength = 600;
    int tableWidth = 40;
    int postHeight = 10;
    int postWidth = 10;
	Dimension size;					 // The size of the applet
	Image buffer;					 // The off-screen image for double-buffering
	Graphics bufferGraphics;		 // A Graphics object for the buffer
	Thread animator;				 // Thread that performs the animation
	Color background;
	boolean please_stop;    // A flag asking animation thread to stop
    Rectangle table;
    boolean[] applyFriction;
    boolean[] bounce;
    
    //set dt
    double t = 0.0, dt = 0.01, g = 9.81;
    double[][] v = new double[n][2];
    double[][] a = new double[n][2];
    double[][] f = new double[n][2]; //total force vector (x and y)
    //physical constants
    double k = 2000.0, m = 2.0, mu = 0.5;
    //spring force magnitudes/angles (left and right)
    double fSR, fSL, thetaR, thetaL; 

    public void init() {
        t = 0.0;
        bounce = new boolean[n];
        //initialize position, velocity, accel, force vectors
        for (int i = 0; i < n; i++) {
            x[i] = tableLength + 350 - 8.0*i;
            y[i] = 60.0 - postHeight;
            v[i][0] = 0.0;
            v[i][1] = 0.0;
            a[i][0] = 0.0;
            a[i][1] = 0.0;
            f[i][0] = 0.0;
            f[i][1] = 0.0;
        }
        //equilibrium distance between beads
        d_eq = Math.hypot(y[0] - y[1], x[0] - x[1]);
        //set dimensions of the applet. second parameter changes where the ground is.
        setSize(1300,600);
		size = this.size();
        buffer = this.createImage(size.width, size.height);
        bufferGraphics = buffer.getGraphics();
        background = Color.white;
        setBackground(background);
        table = new Rectangle(0,65,tableLength, size.height-15);
	}
	
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        bufferGraphics.setClip(g.getClip());
        bufferGraphics.setColor(background);
        bufferGraphics.fillRect(0, 0, 3*size.width, 3*size.height); // clear the buffer
        bufferGraphics.setColor(Color.BLACK);
        //draw table, post, and beads
        bufferGraphics.fillRect(0, 65, tableLength, tableWidth);
        bufferGraphics.fillRect(tableLength - postWidth, 65 - postHeight, postWidth, postHeight);
        for (int i = 0; i < n; i++) {
            bufferGraphics.drawOval((int) Math.round(x[i]), (int) Math.round(y[i]), 5, 5);
        }
        String time = "Time: " + t;
        bufferGraphics.setColor(Color.WHITE);
        //draw time for reference
        bufferGraphics.drawString(time, 100, 100);
        g2.drawImage(buffer, 0, 0, this);
    }
    
    public void update(Graphics g) { paint(g); }
    
    public boolean[] collisionCheck() {
	    boolean[] applyFriction = new boolean[n];
        //beadsCollisionCheck();
        for (int i = 0; i < n; i++) {
            applyPostImpulse(i);
            applyTableImpulse(i);
            if (y[i] > size.height - 10 && !bounce[i]) {
                applyGroundImpulse(i);
                applyFriction[i] = true;
                bounce[i] = true;
            }
            else if (bounce[i]) {
                bounce[i] = false;
            }
        }
        return applyFriction;
    }

    public void beadsCollisionCheck() {
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (Math.hypot(x[j] - x[i], y[j] - y[i]) < 5.0) {
                    applyBeadsImpulse(i,j);
                }
            }
        }
    }

    public void applyPostImpulse(int i) {
        double resCoef = 0.1;


        if (y[i] > (60 - postHeight) && y[i] < 60 && x[i] > tableLength - (postWidth + 5) && x[i] < tableLength) {
            //penetration lengths
            double dy = y[i] - (60 - postHeight);
            double dx, newX;
            //determine penetration in x direction depending on where exactly the bead is
            if (x[i] > tableLength - 7.5) {
                dx = tableLength - x[i];
                newX = tableLength;
            } else {
                dx = x[i] - tableLength + (postWidth + 5);
                newX = tableLength - (postWidth + 5);
            }
            if (dx > dy) {
                y[i] = (60 - postHeight);
                v[i][1] = -v[i][1] * resCoef;
            } else if (dy > dx) {
                x[i] = newX;
                v[i][0] = -v[i][0] * resCoef;
            } else if (dx == dy) {
                System.out.println("degenerate");
                x[i] = newX;    
                y[i] = 35;
            }
        }
    }

    public void applyBeadsImpulse(int i, int j) {
        //collision between beads. not being run now.
        double resCoef = 0.3;
        double diam = 5.0;
        double[] collisionNormal = new double[2];
        collisionNormal[0] = x[j] - x[i];
        collisionNormal[1] = y[j] - y[i];
        double mag = Math.sqrt(dot(collisionNormal, collisionNormal));
        collisionNormal[0] /= mag;
        collisionNormal[1] /= mag;
        double[] vRel = new double[2];
        vRel[0] = v[j][0] - v[i][0];
        vRel[1] = v[j][1] - v[i][1];
        
        double imp = (-(1+resCoef) * (dot(vRel, collisionNormal))) / (dot(collisionNormal, collisionNormal) * 2 / m); 

        v[i][0] -= (imp * collisionNormal[0] / m);
        v[i][1] -= (imp * collisionNormal[1] / m);
        v[j][0] += (imp * collisionNormal[0] / m);
        v[j][1] += (imp * collisionNormal[1] / m);

   }     

    public double dot(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i]*b[i];
        }
        return sum;
    }          

    public void applyTableImpulse(int i) {
        double resCoef = 0.1;

        if (x[i] < tableLength && y[i] > 60 && y[i] < (60 + tableWidth)) {
            double dx = tableLength - x[i];
            double dy, newY;
            //works similarly to post impulse code
            if (y[i] > (60 + (tableWidth / 2))) {
                dy = (60 + tableWidth) - y[i];
                newY = (60 + tableWidth);
            } else {
                dy = y[i] - 60;
                newY = 60.0;
            }
            if (dx > dy) {
                y[i] = newY;
                v[i][1] = -v[i][1] * resCoef;
            } else if (dy > dx) {
                x[i] = tableLength;
                v[i][0] = -v[i][0] * resCoef;
            } else if (dx == dy) {
                x[i] = tableLength;
                y[i] = 60;
            }
        }
    }

    public void applyGroundImpulse(int i) {
        double resCoef = 0.2;

        v[i][1] = -v[i][1] * resCoef;
        if (y[i] > size.height - 10) {
            y[i] = size.height - 10;
        }
    }

    public void run() {
        while(!please_stop) {
            //update physics
            t += dt;

            applyFriction = collisionCheck();

            fSL = -k * (Math.hypot(x[0] - x[1], y[0] - y[1]) - d_eq);
            thetaL = Math.atan2(y[1] - y[0], x[0] - x[1]);
            f[0][0] = fSL * Math.cos(thetaL);
            f[0][1] = fSL * -Math.sin(thetaL);
            f[0][1] += m * g;
            if (applyFriction[0]) {
                f[0][0] += mu * m * g * -Math.signum(v[0][0]);
            }
            a[0][0] = f[0][0] / m;
            a[0][1] = f[0][1] / m;
            
            for (int i = 1; i < n - 1; i++) {
                fSR = -k * (Math.hypot(x[i-1] - x[i], y[i-1] - y[i]) - d_eq);
                thetaR = Math.atan2(y[i] - y[i-1], x[i-1] - x[i]);
                f[i][0] = -fSR * Math.cos(thetaR);
                f[i][1] = fSR * Math.sin(thetaR);
                fSL = -k * (Math.hypot(x[i] - x[i+1], y[i] - y[i+1]) - d_eq);
                thetaL = Math.atan2(y[i+1] - y[i], x[i] - x[i+1]);
                f[i][0] += fSL * Math.cos(thetaL);
                f[i][1] += fSL * -Math.sin(thetaL);
                f[i][1] += m * g;
                if (applyFriction[i]) {
                    f[i][0] += mu * m * g * -Math.signum(v[i][0]);
                }
                a[i][0] = f[i][0] / m;
                a[i][1] = f[i][1] / m;
            }
            
            fSR = -k * (Math.hypot(x[n - 1] - x[n - 2], y[n - 1] - y[n - 2]) - d_eq);
            thetaR = Math.atan2(y[n - 1] - y[n - 2], x[n - 2] - x[n - 1]);
            f[n-1][0] = -fSR * Math.cos(thetaR);
            f[n-1][1] = fSR * Math.sin(thetaR);
            f[n-1][1] += m * g;
            if (applyFriction[n-1]) {
                f[n-1][0] += mu * m * g * -Math.signum(v[n-1][0]);
            }
            a[n-1][0] = f[n-1][0] / m;
            a[n-1][1] = f[n-1][1] / m;

            //Euler method for velocity and position
            for (int i = 0; i < n; i++) {
                v[i][0] += a[i][0] * dt;
                v[i][1] += a[i][1] * dt;
                x[i] += v[i][0] * dt;
                y[i] += v[i][1] * dt;
            }

            try {   
                Thread.sleep(1);
            } catch (InterruptedException e) { ; }
            repaint();
        }
        animator = null;
    }
    
	/** Start the animation thread */
	public void start() {
		if (animator == null) {
			please_stop = false;
			animator = new Thread(this);
			animator.start();
		}
	}

	/** Stop the animation thread */
	public void stop() { please_stop = true; }

	/** Allow the user to start and stop the animation by clicking */
	public boolean mouseDown(Event e, int x, int y) {
		if (animator != null) { please_stop = true; } // if running request a stop
		else { start(); }							   // otherwise start it.
		return true;
	}
}	 
