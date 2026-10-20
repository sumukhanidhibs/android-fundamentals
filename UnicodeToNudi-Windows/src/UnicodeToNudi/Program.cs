using System;
using System.Threading;
using System.Windows.Forms;

namespace UnicodeToNudi
{
    internal static class Program
    {
        /// <summary>Passed when started at sign-in, to skip the "is running" message.</summary>
        public const string BackgroundArg = "/background";

        [STAThread]
        private static void Main(string[] args)
        {
            // Only one copy may own the shortcut
            using (var mutex = new Mutex(true, @"Local\UnicodeToNudi", out bool firstInstance))
            {
                if (!firstInstance)
                {
                    MessageBox.Show("Unicode to Nudi is already running. Look for its icon near the clock.",
                        "Unicode to Nudi", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    return;
                }

                Application.EnableVisualStyles();
                Application.SetCompatibleTextRenderingDefault(false);
                bool quiet = args.Length > 0 && args[0] == BackgroundArg;
                Application.Run(new TrayApp(quiet));
            }
        }
    }
}
