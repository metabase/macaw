((nil . ((indent-tabs-mode . nil)       ; always use spaces for tabs
         (require-final-newline . t)))  ; add final newline on save
 (clojure-mode . ((cider-preferred-build-tool . clojure-cli)
                  (cider-clojure-cli-aliases . "dev")
                  (cljr-favor-prefix-notation . nil)
                  (fill-column . 120)
                  (column-enforce-column . 120)
                  (clojure-docstring-fill-column . 120)
                  (eval . (put-clojure-indent 'with-meta '(:form)))
                  (eval . (put-clojure-indent 'with-bindings* '(:form)))))
 (markdown-mode . ((fill-column . 80)
                   (column-enforce-column . 80))))
