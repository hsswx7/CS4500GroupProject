// Repurposed for River Kidneys Map
/*

Created by Rebecca Dolph

*/

#include <gl/glut.h>
#include <stdio.h>
#include <vector>
#include <fstream>

#include "RGBpixmap.h"

using namespace std;

#define IMAGE_X 300
#define IMAGE_Y 350

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
	gluOrtho2D(0.0, IMAGE_X, 0.0, IMAGE_Y);
}



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
		if (x <= IMAGE_X && x >= 0 && y <= IMAGE_Y && y >= 0) {
			points.push_back(x); // add mouse location to vector
			points.push_back(y);
		}
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
	if (x <= IMAGE_X && x >= 0 && y <= IMAGE_Y && y >= 0) {
		points.push_back(x); // add mouse locations to vector
		points.push_back(y);
	}
	glutPostRedisplay();
}

void myKeyboard(unsigned char Key, int mouseX, int mouseY) {
	ofstream myfile;
	switch (Key) {
		case 'c':
			myfile.open("points.txt");
			for (size_t i = 0; i < points.size(); i += 2)
			{
				myfile << points[i + 0] << " " << points[i + 1] << "\n";
				
			}
			myfile.close();
			break;
		case 'x':
			exit(0);
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
	glOrtho(0.0f, IMAGE_X, IMAGE_Y, 0.0, 0.0, 1.f);

	glEnable(GL_TEXTURE_2D); 
	glBindTexture(GL_TEXTURE_2D, 2001);

	glColor3f(1.0f, 1.0f, 1.0f);
	glBegin(GL_QUADS);
	glTexCoord2d(0.0, 0.0); glVertex2d(0.0, IMAGE_Y);
	glTexCoord2d(1.0, 0.0); glVertex2d(IMAGE_X, IMAGE_Y);
	glTexCoord2d(1.0, 1.0); glVertex2d(IMAGE_X, 0.0);
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
	MessageBox(NULL, L"This program lets you draw on a map and store the coords of your line.\n\n- To draw, left click points or left click and drag.\n- To start over, right click.\n- To save your line to file, press 'c'.\n- To exit, press 'x'.", L"River Tracer", MB_ICONASTERISK);
	glutInit(&argc, argv);
	glutInitDisplayMode(GLUT_SINGLE | GLUT_RGB);
	glutInitWindowSize(IMAGE_X, IMAGE_Y);
	glutInitWindowPosition(100, 150);
	glutCreateWindow("Freehand");

	glutMouseFunc(myMouse);
	glutMotionFunc(myMovedMouse);
	glutKeyboardFunc(myKeyboard);
	glutDisplayFunc(myDisplay);

	myInit();
	glutMainLoop();
}
