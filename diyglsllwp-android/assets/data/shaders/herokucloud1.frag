
 /*  @title 	Cloud Demo 
 *  @version 	v0.3
 *  @author 	Mark Sleith
 *  @website 	www.cngames.co.uk/portfolio
 *  @date 	15/08/2012
 *
 *  @note	Noise and fBm from iq's latest live coding video, "a simple eye ball".
 *
 *  @todo	Add varying cloud density, cloud illumination.
 */


#ifdef GL_ES
	precision lowp float;
#endif

#define CLOUD_COVER		0.75
#define CLOUD_SHARPNESS		0.035
uniform float 		time;
uniform vec2  		mouse;
uniform vec2  		resolution;

uniform sampler2D	tex0;

float hash( float n )
{
	return fract(sin(n)*43758.5453);
}

float noise( in vec2 x )
{
	vec2 p = floor(x);
	vec2 f = fract(x);
    	f = f*f*(3.0-2.0*f);
    	float n = p.x + p.y*57.0;
    	float res = mix(mix( hash(n+  0.0), hash(n+  1.0),f.x), mix( hash(n+ 57.0), hash(n+ 58.0),f.x),f.y);
    	return res;
}

float fbm( vec2 p )
{
    	float f = 0.0;
    	f += 0.50000*noise( p ); p = p*2.02;
    	f += 0.25000*noise( p+time ); p = p*2.03;
    	f += 0.12500*noise( p+time/2.0 ); p = p*2.01;
    	f += 0.06250*noise( p); p = p*2.04;
    	f += 0.03125*noise( p );
    	return f/0.984375;
}

// Entry point
void main( void ) {
	// Wind - Used to animate the clouds
	vec2 wind_vec = vec2(0.001 + time*0.01, 0.003 + time * 0.01);
	
	// Enable raytracing
	bool enable_sun = false;
	
	// Set suns position to mouse coords
	vec3 sun_vec = vec3(mouse, -20.0);
	
	
	// Set up domain
	vec2 q = ( gl_FragCoord.xy / resolution.xy );
	vec2 p = -1.0 + 3.0 * q + wind_vec;
	
	// Fix aspect ratio
	p.x *= resolution.x / resolution.y;

	
	// Create noise using fBm
	float f = fbm( 4.0*p );

	float cover = CLOUD_COVER;
	float sharpness = CLOUD_SHARPNESS;
	
	float c = f - (1.0 - cover);
	if ( c < 0.0 )
		c = 0.0;
	
	f = 1.0 - (pow(sharpness, c));
	
	
	
	// If raytracing enabled
	if(enable_sun)
	{
		float	Scattering = 0.0;
		vec3	EndTracePos = vec3(gl_FragCoord.st, -f);				// Get cloud voxel
		vec3	TraceDir = EndTracePos - sun_vec;//vec3(0.5, 0.5, sun_vec.y);			// Get trace direction
		TraceDir = normalize(TraceDir);							// Normalize it
		vec3	CurTracePos = vec3(0.5, 0.5, sun_vec.y) + TraceDir * 0.025;		// Scale trace position

		// Approximate light scattering integral from sun to current cloud voxel.
		TraceDir *= 2.0;
		for(int i=0; i<4; i++)								// OPTIMIZATION, less raymarching! Originally 64, 16 for speed atm.
		{
			CurTracePos += TraceDir;
			//vec4 tex2 = vec4(f);
			vec4	tex2 = texture2D(tex0, CurTracePos.xy);		// TODO: It's looking up the texel not the fragment, gotta figure out which tex location I can store the clouds on this site.
			Scattering += (.1/255.0) * step(CurTracePos.z*2.0, tex2.r*2.0);		// Check if ray is inside clouds
		}
		
		

		float	Light = 1.0 / exp(Scattering * 0.4);
		f = Light;
	}
	

	gl_FragColor = vec4( f, f, 1.0/f, f );
}