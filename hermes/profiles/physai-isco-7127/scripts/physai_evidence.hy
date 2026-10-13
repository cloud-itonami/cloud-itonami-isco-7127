"Evidence for a physical-AI bot's growth tick: the same measurement as\nphysai_measure.py, printed for the agent and always exit 0.\n\nA Hermes cron script that exits non-zero reaches the agent as a failure\nmessage instead of as evidence; a red tick is exactly when the agent must read\nit, so the status travels in the text (STATUS line), not in the exit code.\n"
(import os)
(import subprocess)
(import sys)
(setv PROFILE ((. os.path basename) ((. os.path dirname) ((. os.path dirname) ((. os.path abspath) __file__)))))
(setv ROOT ((. os.environ get) "ITONAMI_ROOT" ((. os.path expanduser) "~/github/com-junkawasaki")))
(setv KBB ((. os.environ get) "KBB" "/opt/homebrew/bin/kbb"))
(setv TICK ((. os.path join) ROOT "scripts" "physical-ai-bots" "tick.cljk"))
(when (= __name__ "__main__") (setv r (subprocess.run [KBB "--backend" "sci" TICK "evidence" PROFILE "--hermes"] :cwd ROOT :capture_output True :text True)) ((. sys.stdout write) r.stdout) (when (!= r.returncode 0) (print f"STATUS\tUNMEASURED\nERROR\ttick.cljk exited {r.returncode}: {(cut ((. r.stderr strip)) -600 None)}")) (raise (SystemExit 0)))
