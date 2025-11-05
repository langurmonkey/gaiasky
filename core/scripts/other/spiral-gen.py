import numpy as np
import matplotlib.pyplot as plt
from matplotlib.widgets import Slider, Button
import matplotlib as mpl
mpl.use('Qt5Agg')  # Use Qt backend which handles HDPI better
import matplotlib.pyplot as plt

# Set DPI for better scaling
mpl.rcParams['figure.dpi'] = 50
mpl.rcParams['savefig.dpi'] = 50

class SpiralArmSimulator:
    def __init__(self):
        # Default parameters (matching your GLSL code)
        self.u_baseRadius = 1.0
        self.r_min = 0.2 * self.u_baseRadius
        self.r_max = 1.3 * self.u_baseRadius
        self.armWidth = 0.1 * self.u_baseRadius
        self.numArms = 2
        self.pitchAngleDeg = 25.0
        self.num_particles = 6000
        self.heightScale = 0.1
        
        # Parameters we want to control
        self.sigma = 0.1
        self.wobbleWidth = 0.21 * self.u_baseRadius
        self.wobbleFreq1 = 23.1
        self.wobbleFreq2 = 14.25
        self.wobbleAmp1 = 0.877
        self.wobbleAmp2 = 0.433

        self.clumpFreq1 = 13.0
        self.clumpFreq2 = 9.0
        self.clumpBias = 0.05
        
        self.fig, self.ax = plt.subplots(figsize=(12, 12))
        plt.subplots_adjust(left=0.1, bottom=0.35)
        
        self.setup_sliders()
        self.generate_particles()
        self.plot()
        
    def gaussian(self, size):
        """Box-Muller transform for Gaussian distribution"""
        u1 = np.random.random(size)
        u2 = np.random.random(size)
        return np.sqrt(-2.0 * np.log(u1)) * np.cos(2.0 * np.pi * u2)
    
    def noise(self, x):
        """Simple 1D noise function"""
        return np.sin(x * 12.9898 + 78.233) * 43758.5453 % 1.0

    def should_place_particle(self, spiralAngle, r, armIndex):
        """Return True if particle should be placed in clump, False for gap"""
        clump_freq1 = self.clumpFreq1
        clump_freq2 = self.clumpFreq2  
        clump_bias = self.clumpBias
    
        # Add arm-specific phase to make each arm unique
        arm_phase = armIndex * 1.618  # Golden ratio for good distribution
    
        # Create pattern with arm-specific variation
        base_pattern = (np.sin((spiralAngle + arm_phase) * clump_freq1) + 
                       np.cos((spiralAngle + arm_phase) * clump_freq2) + 1.0) / 2.0
    
        # Add some random noise to break perfect patterns
        random_noise = np.random.random() * 0.3 - 0.15  # ±0.15 noise
        base_pattern = np.clip(base_pattern + random_noise, 0.0, 1.0)
    
        # Apply bias
        probability = np.clip(base_pattern + clump_bias, 0.0, 1.0)
    
        probability = np.clip(probability, 0.0, 1.0)
    
        return np.random.random() < probability
    
    def generate_particles(self):
        """Generate spiral arm particles based on current parameters"""
        pitchRad = np.radians(np.clip(self.pitchAngleDeg, 1.0, 80.0))
        b = 1.0 / np.tan(pitchRad)
    
        particles_needed = self.num_particles
        all_r = np.array([])
        all_armOffsets = np.array([])
        all_spiralAngle = np.array([])
        all_armIndices = np.array([])
    
        # Keep generating until we have enough particles that pass the filter
        while len(all_r) < particles_needed:
            # Generate candidate particles
            r = np.random.uniform(self.r_min, self.r_max, particles_needed)
            # Denstiy falloff with radius
            radius_factors = 1.0 + 0.5 * (r - self.r_min) / (self.r_max - self.r_min)
            r = self.r_min + (self.r_max - self.r_min) * np.random.random(particles_needed) ** radius_factors

            armIndices = np.random.randint(0, self.numArms, particles_needed)
            armOffsets = armIndices * 2 * np.pi / self.numArms
            spiralAngle = b * np.log(r / (0.1 * self.u_baseRadius)) + armOffsets
        
            # Apply clump mask
            clump_mask = np.array([self.should_place_particle(sa, rad, ai) 
                                  for sa, rad, ai in zip(spiralAngle, r, armIndices)])
        
            # Add only the particles that pass the filter
            valid_indices = np.where(clump_mask)[0]
            all_r = np.concatenate([all_r, r[valid_indices]])
            all_armOffsets = np.concatenate([all_armOffsets, armOffsets[valid_indices]])
            all_spiralAngle = np.concatenate([all_spiralAngle, spiralAngle[valid_indices]])
            all_armIndices = np.concatenate([all_armIndices, armIndices[valid_indices]])
    
        # Take exactly the number we need
        r = all_r[:particles_needed]
        armOffsets = all_armOffsets[:particles_needed]
        spiralAngle = all_spiralAngle[:particles_needed]
        armIndices = all_armIndices[:particles_needed]
    
        # Compute directions
        radialDir = np.column_stack([np.cos(spiralAngle), np.sin(spiralAngle)])
        tangentDir = np.column_stack([-radialDir[:, 1] - b * radialDir[:, 0], 
                                    radialDir[:, 0] - b * radialDir[:, 1]])
        # Normalize tangent directions
        tangent_norms = np.linalg.norm(tangentDir, axis=1, keepdims=True)
        tangentDir = tangentDir / tangent_norms
        normalDir = np.column_stack([-tangentDir[:, 1], tangentDir[:, 0]])
        
        # Wobble displacement
        wobble1 = np.sin(r * self.wobbleFreq1 + armOffsets) * self.wobbleAmp1
        wobble2 = np.cos(r * self.wobbleFreq2 + armOffsets * 1.5) * self.wobbleAmp2
        totalWobble = wobble1 + wobble2
        wobbleDisplacement = (totalWobble * self.wobbleWidth)[:, np.newaxis] * normalDir
        
        # Gaussian arm spread
        gaussianSpread = self.gaussian(self.num_particles) * self.sigma
        armSpread = (gaussianSpread * self.armWidth)[:, np.newaxis] * tangentDir
        
        # Combine displacements
        totalDisplacement = armSpread + wobbleDisplacement
        
        # Final positions
        x = r * np.cos(spiralAngle) + totalDisplacement[:, 0]
        z = r * np.sin(spiralAngle) + totalDisplacement[:, 1]
        y = (np.random.random(self.num_particles) - 0.5) * 2.0 * self.heightScale
        
        self.particles = np.column_stack([x, z])
        
    def plot(self):
        """Plot the current particle distribution"""
        self.ax.clear()
        self.ax.scatter(self.particles[:, 0], self.particles[:, 1], 
                       s=0.5, alpha=0.4, c='blue')
        self.ax.set_xlim(-self.r_max*1.5, self.r_max*1.5)
        self.ax.set_ylim(-self.r_max*1.5, self.r_max*1.5)
        self.ax.set_aspect('equal')
        self.ax.set_title('Spiral Arm Particle Distribution')
        self.ax.set_xlabel('X')
        self.ax.set_ylabel('Z')
        self.ax.grid(True, alpha=0.5)
        
        self.fig.canvas.draw_idle()
    
    def setup_sliders(self):
        """Setup the interactive sliders"""
        axcolor = 'lightgoldenrodyellow'
    
        # Create slider positions - two columns
        slider_height = 0.03
        slider_spacing = 0.04
        start_y = 0.25
        left_col = 0.12
        right_col = 0.64
        col_width = 0.26
    
        # Left column - Wobble parameters
        ax_sigma = plt.axes([left_col, start_y, col_width, slider_height], facecolor=axcolor)
        ax_wobble_width = plt.axes([left_col, start_y - slider_spacing, col_width, slider_height], facecolor=axcolor)
        ax_wobble_freq1 = plt.axes([left_col, start_y - 2*slider_spacing, col_width, slider_height], facecolor=axcolor)
        ax_wobble_freq2 = plt.axes([left_col, start_y - 3*slider_spacing, col_width, slider_height], facecolor=axcolor)
        ax_wobble_amp1 = plt.axes([left_col, start_y - 4*slider_spacing, col_width, slider_height], facecolor=axcolor)
        ax_wobble_amp2 = plt.axes([left_col, start_y - 5*slider_spacing, col_width, slider_height], facecolor=axcolor)
        ax_particles = plt.axes([left_col, start_y - 6*slider_spacing, col_width, slider_height], facecolor=axcolor)
    
        # Right column - Clump parameters
        ax_clump_freq1 = plt.axes([right_col, start_y, col_width, slider_height], facecolor=axcolor)
        ax_clump_freq2 = plt.axes([right_col, start_y - slider_spacing, col_width, slider_height], facecolor=axcolor)
        ax_clump_bias = plt.axes([right_col, start_y - 2*slider_spacing, col_width, slider_height], facecolor=axcolor)
    
        # Create sliders - Left column
        self.slider_sigma = Slider(ax_sigma, 'Sigma', 0.1, 3.0, valinit=self.sigma)
        self.slider_wobble_width = Slider(ax_wobble_width, 'Wobble Width', 0.01, 0.25, valinit=self.wobbleWidth)
        self.slider_wobble_freq1 = Slider(ax_wobble_freq1, 'Wobble Freq 1', 1.0, 20.0, valinit=self.wobbleFreq1)
        self.slider_wobble_freq2 = Slider(ax_wobble_freq2, 'Wobble Freq 2', 1.0, 20.0, valinit=self.wobbleFreq2)
        self.slider_wobble_amp1 = Slider(ax_wobble_amp1, 'Wobble Amp 1', 0.1, 3.0, valinit=self.wobbleAmp1)
        self.slider_wobble_amp2 = Slider(ax_wobble_amp2, 'Wobble Amp 2', 0.1, 3.0, valinit=self.wobbleAmp2)
        self.slider_particles = Slider(ax_particles, 'Particles (k)', 1, 10, valinit=self.num_particles/1000)
    
        # Create sliders - Right column
        self.slider_clump_freq1 = Slider(ax_clump_freq1, 'Clump Freq 1', 1.0, 20.0, valinit=self.clumpFreq1)
        self.slider_clump_freq2 = Slider(ax_clump_freq2, 'Clump Freq 2', 1.0, 20.0, valinit=self.clumpFreq2)
        self.slider_clump_bias = Slider(ax_clump_bias, 'Clump Bias', -1.0, 1.0, valinit=self.clumpBias)
    
        # Connect sliders to update function
        self.slider_sigma.on_changed(self.update)
        self.slider_wobble_width.on_changed(self.update)
        self.slider_wobble_freq1.on_changed(self.update)
        self.slider_wobble_freq2.on_changed(self.update)
        self.slider_wobble_amp1.on_changed(self.update)
        self.slider_wobble_amp2.on_changed(self.update)
        self.slider_particles.on_changed(self.update)
        self.slider_clump_freq1.on_changed(self.update)
        self.slider_clump_freq2.on_changed(self.update)
        self.slider_clump_bias.on_changed(self.update)
    
    
    def update(self, val):
        """Update the plot when sliders change"""
        self.sigma = self.slider_sigma.val
        self.wobbleWidth = self.slider_wobble_width.val
        self.wobbleFreq1 = self.slider_wobble_freq1.val
        self.wobbleFreq2 = self.slider_wobble_freq2.val
        self.wobbleAmp1 = self.slider_wobble_amp1.val
        self.wobbleAmp2 = self.slider_wobble_amp2.val
        self.num_particles = int(self.slider_particles.val * 1000)
        self.clumpFreq1 = self.slider_clump_freq1.val
        self.clumpFreq2 = self.slider_clump_freq2.val
        self.clumpBias = self.slider_clump_bias.val
        
        self.generate_particles()
        self.plot()
    
    def reset(self, event):
        """Reset sliders to default values"""
        self.slider_sigma.reset()
        self.slider_wobble_width.reset()
        self.slider_wobble_freq1.reset()
        self.slider_wobble_freq2.reset()
        self.slider_wobble_amp1.reset()
        self.slider_wobble_amp2.reset()
        self.slider_particles.reset()
        self.slider_clump_freq1.reset()
        self.slider_clump_freq2.reset()
        self.slider_clump_bias.reset()

# Run the interactive plot
if __name__ == "__main__":
    simulator = SpiralArmSimulator()
    plt.show()
