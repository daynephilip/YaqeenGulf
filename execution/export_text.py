"""
export_text.py

Exports fine-tuned sequence classification BERT models to ONNX:
1. text_en.onnx (from D:\\ml_export\\checkpoints\\text_en\\bertbase_isot.pt, bert-base-uncased)
2. text_ar.onnx (from D:\\ml_export\\checkpoints\\text_ar\\arabert_afnd.pt, aubmindlab/bert-base-arabertv02)

Explicit outputs:
- 'logits': [batch, 2]
- 'attention': [batch, num_heads, seq_len, seq_len] (final encoder layer attention matrix, for token highlights)

Also saves vocabulary files to D:\\ml_export\\vocab\\ and app\\src\\main\\assets\\vocab\\:
- vocab_en.txt
- vocab_ar.txt
"""

import os
import sys

os.environ["USE_TF"] = "0"
os.environ["TRANSFORMERS_NO_TF"] = "1"
os.environ["HF_HUB_DISABLE_SYMLINKS_WARNING"] = "1"

if sys.platform == "win32" and getattr(sys.stdout, "encoding", "").lower() != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

import torch
import torch.nn as nn
from transformers import AutoModelForSequenceClassification, AutoTokenizer

MODELS = [
    {
        "name": "text_en",
        "base_model": "bert-base-uncased",
        "ckpt": r"D:\ml_export\checkpoints\text_en\bertbase_isot.pt",
        "output": r"D:\ml_export\onnx_out\text_en.onnx",
        "vocab_out": r"D:\ml_export\vocab\vocab_en.txt",
    },
    {
        "name": "text_ar",
        "base_model": "aubmindlab/bert-base-arabertv02",
        "ckpt": r"D:\ml_export\checkpoints\text_ar\arabert_afnd.pt",
        "output": r"D:\ml_export\onnx_out\text_ar.onnx",
        "vocab_out": r"D:\ml_export\vocab\vocab_ar.txt",
    },
]


class BertWithFinalAttention(nn.Module):
    def __init__(self, base_model):
        super().__init__()
        self.model = base_model

    def forward(self, input_ids, attention_mask):
        out = self.model(
            input_ids=input_ids,
            attention_mask=attention_mask,
            output_attentions=True,
            return_dict=True,
        )
        logits = out.logits  # (B, 2)
        final_attention = out.attentions[-1]
        return logits, final_attention


def export():
    os.makedirs(r"D:\ml_export\onnx_out", exist_ok=True)
    os.makedirs(r"D:\ml_export\vocab", exist_ok=True)

    seq_len = 64
    dummy_input_ids = torch.ones(1, seq_len, dtype=torch.long)
    dummy_attention_mask = torch.ones(1, seq_len, dtype=torch.long)

    for item in MODELS:
        print(f"\n==========================================")
        print(f"Processing {item['name']} ({item['base_model']})")
        print(f"==========================================")

        print(f"Loading tokenizer: {item['base_model']}")
        tokenizer = AutoTokenizer.from_pretrained(item["base_model"])
        print(f"Saving vocabulary to {item['vocab_out']}")
        tokenizer.save_vocabulary(os.path.dirname(item["vocab_out"]))
        default_vocab = os.path.join(os.path.dirname(item["vocab_out"]), "vocab.txt")
        if os.path.exists(default_vocab):
            if os.path.exists(item["vocab_out"]) and default_vocab != item["vocab_out"]:
                os.remove(item["vocab_out"])
            os.rename(default_vocab, item["vocab_out"])

        print(f"Instantiating model from {item['base_model']}")
        base_model = AutoModelForSequenceClassification.from_pretrained(
            item["base_model"],
            num_labels=2,
            output_attentions=True,
        )

        print(f"Loading weights from checkpoint: {item['ckpt']}")
        ckpt = torch.load(item["ckpt"], map_location="cpu")
        state_dict = ckpt["model_state_dict"] if "model_state_dict" in ckpt else (
            ckpt["model_state"] if "model_state" in ckpt else ckpt
        )

        base_model.load_state_dict(state_dict)
        base_model.eval()

        wrapped_model = BertWithFinalAttention(base_model)
        wrapped_model.eval()

        print(f"Exporting to ONNX: {item['output']}")
        torch.onnx.export(
            wrapped_model,
            (dummy_input_ids, dummy_attention_mask),
            item["output"],
            export_params=True,
            opset_version=14,
            do_constant_folding=True,
            input_names=["input_ids", "attention_mask"],
            output_names=["logits", "attention"],
            dynamic_axes={
                "input_ids": {0: "batch_size", 1: "seq_len"},
                "attention_mask": {0: "batch_size", 1: "seq_len"},
                "logits": {0: "batch_size"},
                "attention": {0: "batch_size", 2: "seq_len", 3: "seq_len"},
            },
            dynamo=False,
        )
        print(f"Successfully exported {item['output']}")


if __name__ == "__main__":
    export()
