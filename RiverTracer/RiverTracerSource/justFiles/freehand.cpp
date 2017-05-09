// Repurposed for River Kidneys Map
/*

TO USE:
Run program, a map will be displayed.
Draw along the river, your mouse coordinates are stored in a vector.

When you are satisfied with your line, press 'x', and the coords
will be printed to a file named "points.txt"

If you want to start over, right click on your mouse.


*/

#include <gl/glut.h>
#include <stdio.h>
#include <vector>
#include <fstream>

#include "RGBpixmap.h"

using namespace std;


RGBpixmap pix[1]; // store the texture
vector< int > points; // using a vector to store all points mouse uses
void myInit(void)
{
	glColor3f(1.0f, 0.0f, 0.0f);
	//glEnable(GL_DEPTH_TEST);
	glEnable(GL_TEXTURE_2D);

	pix[0].readBMPFile("river.bmp");
	pix[0].setTexture(2001);

	glMatrixMode(GL_PROJECTION);
	glLoadIdentity();
	gluOrtho2D(0.0, 304, 0.0, 354);
}

#define IMAGE_X 304
#define IMAGE_Y 354

void myMouse(int button, int state, int mouseX, int mouseY) // determine what to do with which mouse click
{
	int x = mouseX;
	int y = mouseY;
	if (button == GLUT_LEFT_BUTTON)
	{
		/*if (state == GLUT_DOWN)
		{
			points.clear();
		}*/
		points.push_back(x); // add mouse location to vector
		points.push_back(y);
		glutPostRedisplay();
	}
	else if (button == GLUT_RIGHT_BUTTON) // clear canvas
	{
		glClear(GL_COLOR_BUFFER_BIT);
		points.clear();
		glFlush();
	}
}

void myMovedMouse(int mouseX, int mouseY) // if mouse is moving
{
	int x = mouseX;
	int y = mouseY;
	points.push_back(x); // add mouse locations to vector
	points.push_back(y);
	glutPostRedisplay();
}

void myKeyboard(unsigned char Key, int mouseX, int mouseY) {
	switch (Key) {
		case 'x':
			ofstream myfile;
			myfile.open("points.txt");
			for (size_t i = 0; i < points.size(); i += 2)
			{
				myfile << points[i + 0] << " " << points[i + 1] << "\n";
				
			}
			myfile.close();
			break;
		}
}

void myDisplay(void)
{
	glClearColor(0.0, 0.0, 0.0, 1.0);
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

	// background render
	

	glDisable(GL_DEPTH_TEST); ///!!!!
	glMatrixMode(GL_PROJECTION);
	glLoadIdentity();
	glOrtho(0.0f, 304.0, 354.0, 0.0, 0.0, 1.f);

	glEnable(GL_TEXTURE_2D); 
	glBindTexture(GL_TEXTURE_2D, 2001);

	glColor3f(1.0f, 1.0f, 1.0f);
	glBegin(GL_QUADS);
	glTexCoord2d(0.0, 0.0); glVertex2d(0.0, 354.0);
	glTexCoord2d(1.0, 0.0); glVertex2d(304.0, 354.0);
	glTexCoord2d(1.0, 1.0); glVertex2d(304.0, 0.0);
	glTexCoord2d(0.0, 1.0); glVertex2d(0.0, 0.0);

	glEnd();

	glDisable(GL_TEXTURE_2D);

	glBegin(GL_LINE_STRIP);                          // use vector to create the drawn line
	for (size_t i = 0; i < points.size(); i += 2)
	{
		glVertex2i(points[i + 0], points[i + 1]);
	}
	glEnd();

	glutSwapBuffers();

}

void main(int argc, char** argv)
{
	cout << "PRESS 'x' WHEN YOU WANT TO WRITE YOUR LINE TO FILE" << endl;
	glutInit(&argc, argv);
	glutInitDisplayMode(GLUT_SINGLE | GLUT_RGB);
	glutInitWindowSize(304, 354);
	glutInitWindowPosition(100, 150);
	glutCreateWindow("Freehand");

	glutMouseFunc(myMouse);
	glutMotionFunc(myMovedMouse);
	glutKeyboardFunc(myKeyboard);
	glutDisplayFunc(myDisplay);

	myInit();
	glutMainLoop();
}
