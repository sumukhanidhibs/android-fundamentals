using System.Drawing;
using System.Windows.Forms;

namespace UnicodeToNudi
{
    /// <summary>Small "please wait" window shown while Word text is being converted.</summary>
    internal sealed class ProgressForm : Form
    {
        private readonly ProgressBar bar;

        public ProgressForm()
        {
            Text = "Unicode to Nudi " + Program.VersionText;
            FormBorderStyle = FormBorderStyle.FixedDialog;
            ControlBox = false;
            ShowInTaskbar = false;
            TopMost = true;
            StartPosition = FormStartPosition.CenterScreen;
            Font = SystemFonts.MessageBoxFont;
            AutoScaleMode = AutoScaleMode.Font;
            ClientSize = new Size(360, 90);

            var label = new Label
            {
                Text = "Converting to Nudi, please wait...",
                AutoSize = true,
                Location = new Point(16, 16),
            };
            bar = new ProgressBar
            {
                Style = ProgressBarStyle.Marquee,
                MarqueeAnimationSpeed = 30,
                Location = new Point(16, 48),
                Size = new Size(328, 22),
                Minimum = 0,
                Maximum = 100,
            };
            Controls.Add(label);
            Controls.Add(bar);
        }

        // Don't take keyboard focus away from Word
        protected override bool ShowWithoutActivation => true;

        public void SetProgress(int percent)
        {
            if (bar.Style != ProgressBarStyle.Continuous)
            {
                bar.Style = ProgressBarStyle.Continuous;
            }
            bar.Value = percent < 0 ? 0 : percent > 100 ? 100 : percent;
        }
    }
}
