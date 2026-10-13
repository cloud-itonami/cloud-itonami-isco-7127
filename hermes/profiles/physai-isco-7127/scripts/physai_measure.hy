"Measurement tick of a physical-AI bot (no model): run the repo's tests and\nphysics probe on its current main and append one ledger line.\n\nExit code is tick.cljk's own (0 OK / 1 findings / 2 could not measure), so a\nred or unmeasured tick is recorded by the Hermes scheduler as a failed run\nrather than delivered as a quiet success. The bot name is this profile's\ndirectory name; nothing is passed on the command line.\n"
(import os)
(import subprocess)
(import sys)
(setv PROFILE ((. os.path basename) ((. os.path dirname) ((. os.path dirname) ((. os.path abspath) __file__)))))
(setv ROOT ((. os.environ get) "ITONAMI_ROOT" ((. os.path expanduser) "~/github/com-junkawasaki")))
(setv KBB ((. os.environ get) "KBB" "/opt/homebrew/bin/kbb"))
(setv TICK ((. os.path join) ROOT "scripts" "physical-ai-bots" "tick.cljk"))
(when (= __name__ "__main__") (raise (SystemExit (subprocess.call [KBB "--backend" "sci" TICK "evidence" PROFILE] :cwd ROOT))))
