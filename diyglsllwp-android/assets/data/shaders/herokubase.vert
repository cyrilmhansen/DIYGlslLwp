#ifdef GL_ES
precision mediump float;
#endif

attribute vec3 a_position;
attribute vec2 a_texCoord0;

void main()
{
	 gl_Position = vec4(a_position, 1.0 );
} 
