#ifdef GL_ES
precision mediump float;
#endif

attribute vec4 a_position;
uniform mat4 u_mvpMatrix;

void main()
{
    gl_Position = a_position * u_mvpMatrix;
} 
