def compute_binary_metrics(tp, fp, tn, fn):
    acc = (tp + tn) / max(tp + fp + tn + fn, 1)
    prec = tp / max(tp + fp, 1)
    rec = tp / max(tp + fn, 1)
    f1 = 2 * (prec * rec) / max(prec + rec, 1e-7)
    return {'accuracy': acc, 'precision': prec, 'recall': rec, 'f1': f1}
