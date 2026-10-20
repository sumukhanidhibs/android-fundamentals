using System;
using System.Reflection;
using System.Threading;
using System.Windows.Forms;

namespace UnicodeToNudi
{
    internal static class Program
    {
        /// <summary>Passed when started at sign-in, to skip the "is running" message.</summary>
        public const string BackgroundArg = "/background";

        /// <summary>Signalled by a newly started copy to ask the running copy to exit.</summary>
        public const string ExitEventName = @"Local\UnicodeToNudi.Exit";

        private const string MutexName = @"Local\UnicodeToNudi";

        /// <summary>Version shown in the tray tooltip and windows, e.g. "1.0.8".</summary>
        public static string VersionText
        {
            get
            {
                Version v = Assembly.GetExecutingAssembly().GetName().Version;
                return v.Major + "." + v.Minor + "." + v.Build;
            }
        }

        [STAThread]
        private static void Main(string[] args)
        {
            using (var mutex = new Mutex(false, MutexName))
            {
                // Only one copy may own the shortcut. If an older copy is running
                // (e.g. after downloading a new version), ask it to exit and take over.
                if (!Acquire(mutex, TimeSpan.Zero))
                {
                    using (var exitEvent = new EventWaitHandle(false, EventResetMode.AutoReset, ExitEventName))
                    {
                        exitEvent.Set();
                    }
                    if (!Acquire(mutex, TimeSpan.FromSeconds(10)))
                    {
                        MessageBox.Show("Unicode to Nudi is already running and could not be restarted. " +
                                        "Right-click its icon near the clock, choose Exit, and try again.",
                            "Unicode to Nudi", MessageBoxButtons.OK, MessageBoxIcon.Information);
                        return;
                    }
                }

                try
                {
                    Application.EnableVisualStyles();
                    Application.SetCompatibleTextRenderingDefault(false);
                    bool quiet = args.Length > 0 && args[0] == BackgroundArg;
                    Application.Run(new TrayApp(quiet));
                }
                finally
                {
                    mutex.ReleaseMutex();
                }
            }
        }

        private static bool Acquire(Mutex mutex, TimeSpan timeout)
        {
            try
            {
                return mutex.WaitOne(timeout);
            }
            catch (AbandonedMutexException)
            {
                return true; // previous copy ended without releasing it
            }
        }
    }
}
